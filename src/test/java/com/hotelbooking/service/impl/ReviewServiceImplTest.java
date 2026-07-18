package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateReviewRequest;
import com.hotelbooking.dto.response.HotelRatingResponse;
import com.hotelbooking.dto.response.ReviewResponse;
import com.hotelbooking.entity.*;
import com.hotelbooking.entity.enums.BookingStatus;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.BookingNotReviewableException;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.exception.ReviewAlreadyExistsException;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.ReviewRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User guest;
    private Hotel hotel;
    private Booking booking;

    @BeforeEach
    void setUp() {
        guest = User.builder().id(1L).role(Role.USER).build();
        hotel = Hotel.builder().id(1L).build();
        Room room = Room.builder().id(1L)
                .roomType(RoomType.builder().hotel(hotel).basePrice(BigDecimal.TEN).build()).build();
        booking = Booking.builder().id(10L).user(guest).room(room)
                .status(BookingStatus.COMPLETED).totalPrice(BigDecimal.TEN).build();
    }

    @Test
    void create_savesReview_whenBookingCompletedAndOwnedByCaller() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(10L)).thenReturn(false);
        CreateReviewRequest request = new CreateReviewRequest(5, "Great stay");

        ReviewResponse response = reviewService.create(10L, request, new UserPrincipal(guest));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.hotelId()).isEqualTo(1L);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void create_throwsForbidden_whenBookingBelongsToSomeoneElse() {
        User someoneElse = User.builder().id(2L).role(Role.USER).build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        CreateReviewRequest request = new CreateReviewRequest(5, "Great stay");

        assertThatThrownBy(() -> reviewService.create(10L, request, new UserPrincipal(someoneElse)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void create_throwsNotReviewable_whenBookingNotCompleted() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        CreateReviewRequest request = new CreateReviewRequest(5, "Great stay");

        assertThatThrownBy(() -> reviewService.create(10L, request, new UserPrincipal(guest)))
                .isInstanceOf(BookingNotReviewableException.class);
    }

    @Test
    void create_throwsAlreadyExists_whenBookingAlreadyReviewed() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBookingId(10L)).thenReturn(true);
        CreateReviewRequest request = new CreateReviewRequest(5, "Great stay");

        assertThatThrownBy(() -> reviewService.create(10L, request, new UserPrincipal(guest)))
                .isInstanceOf(ReviewAlreadyExistsException.class);
    }

    @Test
    void getHotelRating_returnsAverageAndCount() {
        when(hotelRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findAverageRatingByHotelId(1L)).thenReturn(4.5);
        when(reviewRepository.countByHotelId(1L)).thenReturn(2L);

        HotelRatingResponse response = reviewService.getHotelRating(1L);

        assertThat(response.averageRating()).isEqualTo(4.5);
        assertThat(response.reviewCount()).isEqualTo(2L);
    }

    @Test
    void getHotelRating_throwsResourceNotFound_whenHotelMissing() {
        when(hotelRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.getHotelRating(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
