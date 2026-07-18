package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreatePaymentRequest;
import com.hotelbooking.dto.response.PaymentResponse;
import com.hotelbooking.entity.Booking;
import com.hotelbooking.entity.Payment;
import com.hotelbooking.entity.Room;
import com.hotelbooking.entity.RoomType;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.BookingStatus;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.InvalidPaymentStateException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.PaymentRepository;
import com.hotelbooking.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private User guest;
    private Booking booking;
    private CreatePaymentRequest request;

    @BeforeEach
    void setUp() {
        guest = User.builder().id(1L).role(Role.USER).build();
        Room room = Room.builder().id(1L)
                .roomType(RoomType.builder().basePrice(BigDecimal.TEN).build()).build();
        booking = Booking.builder().id(10L).user(guest).room(room)
                .status(BookingStatus.PENDING).totalPrice(BigDecimal.valueOf(200)).build();
        request = new CreatePaymentRequest(10L, "card");
    }

    @Test
    void pay_confirmsBooking_whenPending() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        PaymentResponse response = paymentService.pay(request, new UserPrincipal(guest));

        assertThat(response.amount()).isEqualByComparingTo("200");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void pay_throwsForbidden_whenCallerIsNotOwnerOrAdmin() {
        User someoneElse = User.builder().id(2L).role(Role.USER).build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.pay(request, new UserPrincipal(someoneElse)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void pay_throwsInvalidState_whenBookingNotPending() {
        booking.setStatus(BookingStatus.CANCELLED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.pay(request, new UserPrincipal(guest)))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void pay_throwsInvalidState_whenAlreadyPaid() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(10L))
                .thenReturn(Optional.of(Payment.builder().id(99L).build()));

        assertThatThrownBy(() -> paymentService.pay(request, new UserPrincipal(guest)))
                .isInstanceOf(InvalidPaymentStateException.class);
    }

    @Test
    void pay_throwsResourceNotFound_whenBookingMissing() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.pay(request, new UserPrincipal(guest)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
