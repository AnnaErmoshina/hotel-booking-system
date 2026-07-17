package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateBookingRequest;
import com.hotelbooking.dto.response.BookingResponse;
import com.hotelbooking.entity.*;
import com.hotelbooking.entity.enums.BookingStatus;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.*;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.RoomRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.cancellation.CancellationPolicy;
import com.hotelbooking.service.pricing.PricingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private PricingStrategy pricingStrategy;
    @Mock
    private CancellationPolicy cancellationPolicy;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User guest;
    private User hotelOwner;
    private Room room;
    private UserPrincipal guestPrincipal;

    @BeforeEach
    void setUp() {
        hotelOwner = User.builder().id(1L).role(Role.HOTEL_MANAGER).build();
        Hotel hotel = Hotel.builder().id(1L).owner(hotelOwner).build();
        RoomType roomType = RoomType.builder().id(1L).hotel(hotel).basePrice(BigDecimal.valueOf(100)).build();
        room = Room.builder().id(1L).roomType(roomType).roomNumber("101").build();

        guest = User.builder().id(2L).role(Role.USER).build();
        guestPrincipal = new UserPrincipal(guest);
    }

    @Test
    void create_savesBooking_whenRoomIsAvailable() {
        CreateBookingRequest request = new CreateBookingRequest(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlappingBooking(1L, request.checkIn(), request.checkOut(), BookingStatus.CANCELLED))
                .thenReturn(false);
        when(pricingStrategy.calculatePrice(room, request.checkIn(), request.checkOut()))
                .thenReturn(BigDecimal.valueOf(200));

        BookingResponse response = bookingService.create(request, guestPrincipal);

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.totalPrice()).isEqualByComparingTo("200");
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void create_throwsRoomNotAvailable_whenDatesOverlap() {
        CreateBookingRequest request = new CreateBookingRequest(1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlappingBooking(eq(1L), any(), any(), eq(BookingStatus.CANCELLED)))
                .thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(request, guestPrincipal))
                .isInstanceOf(RoomNotAvailableException.class);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void create_throwsInvalidDates_whenCheckOutNotAfterCheckIn() {
        CreateBookingRequest request = new CreateBookingRequest(1L, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 3));

        assertThatThrownBy(() -> bookingService.create(request, guestPrincipal))
                .isInstanceOf(InvalidBookingDatesException.class);

        verifyNoInteractions(roomRepository, bookingRepository, pricingStrategy);
    }

    @Test
    void create_throwsResourceNotFound_whenRoomDoesNotExist() {
        CreateBookingRequest request = new CreateBookingRequest(99L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(request, guestPrincipal))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancel_appliesFeeFromPolicy_andSetsStatusCancelled() {
        Booking booking = Booking.builder()
                .id(10L).user(guest).room(room)
                .checkIn(LocalDate.of(2026, 8, 1)).checkOut(LocalDate.of(2026, 8, 3))
                .status(BookingStatus.PENDING).totalPrice(BigDecimal.valueOf(200))
                .build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(cancellationPolicy.calculateFee(eq(booking), any())).thenReturn(BigDecimal.valueOf(50));

        bookingService.cancel(10L, guestPrincipal);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancellationFee()).isEqualByComparingTo("50");
        assertThat(booking.getCancelledAt()).isNotNull();
    }

    @Test
    void cancel_throwsForbidden_whenNotOwnerAndNotAdmin() {
        User someoneElse = User.builder().id(3L).role(Role.USER).build();
        Booking booking = Booking.builder().id(10L).user(someoneElse).room(room)
                .checkIn(LocalDate.now().plusDays(5)).checkOut(LocalDate.now().plusDays(7))
                .status(BookingStatus.PENDING).totalPrice(BigDecimal.TEN).build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(10L, guestPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void cancel_throwsAlreadyCancelled_whenBookingAlreadyCancelledOrCompleted() {
        Booking booking = Booking.builder().id(10L).user(guest).room(room)
                .checkIn(LocalDate.now().plusDays(5)).checkOut(LocalDate.now().plusDays(7))
                .status(BookingStatus.CANCELLED).totalPrice(BigDecimal.TEN).build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(10L, guestPrincipal))
                .isInstanceOf(BookingAlreadyCancelledException.class);
    }

    @Test
    void complete_marksCompleted_whenHotelOwnerAndPastCheckOut() {
        UserPrincipal ownerPrincipal = new UserPrincipal(hotelOwner);
        Booking booking = Booking.builder().id(10L).user(guest).room(room)
                .checkIn(LocalDate.now().minusDays(5)).checkOut(LocalDate.now().minusDays(1))
                .status(BookingStatus.CONFIRMED).totalPrice(BigDecimal.TEN).build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        bookingService.complete(10L, ownerPrincipal);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void complete_throwsForbidden_whenCallerIsGuestNotHotelOwner() {
        Booking booking = Booking.builder().id(10L).user(guest).room(room)
                .checkIn(LocalDate.now().minusDays(5)).checkOut(LocalDate.now().minusDays(1))
                .status(BookingStatus.CONFIRMED).totalPrice(BigDecimal.TEN).build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.complete(10L, guestPrincipal))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void complete_throwsNotCompletable_whenNotYetCheckedOut() {
        UserPrincipal ownerPrincipal = new UserPrincipal(hotelOwner);
        Booking booking = Booking.builder().id(10L).user(guest).room(room)
                .checkIn(LocalDate.now().plusDays(1)).checkOut(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED).totalPrice(BigDecimal.TEN).build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.complete(10L, ownerPrincipal))
                .isInstanceOf(BookingNotCompletableException.class);
    }

    @Test
    void complete_throwsNotCompletable_whenStatusIsNotConfirmed() {
        UserPrincipal ownerPrincipal = new UserPrincipal(hotelOwner);
        Booking booking = Booking.builder().id(10L).user(guest).room(room)
                .checkIn(LocalDate.now().minusDays(5)).checkOut(LocalDate.now().minusDays(1))
                .status(BookingStatus.PENDING).totalPrice(BigDecimal.TEN).build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.complete(10L, ownerPrincipal))
                .isInstanceOf(BookingNotCompletableException.class);
    }
}
