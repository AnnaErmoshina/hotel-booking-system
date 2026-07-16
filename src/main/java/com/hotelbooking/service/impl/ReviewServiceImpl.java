package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateReviewRequest;
import com.hotelbooking.dto.response.HotelRatingResponse;
import com.hotelbooking.dto.response.ReviewResponse;
import com.hotelbooking.entity.Booking;
import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.Review;
import com.hotelbooking.entity.enums.BookingStatus;
import com.hotelbooking.exception.BookingNotReviewableException;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.exception.ReviewAlreadyExistsException;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.HotelRepository;
import com.hotelbooking.repository.ReviewRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;

    @Override
    @Transactional
    public ReviewResponse create(Long bookingId, CreateReviewRequest request, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (!booking.getUser().getId().equals(currentUser.getUser().getId())) {
            throw new ForbiddenOperationException("You can only review your own bookings");
        }

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BookingNotReviewableException(
                    "Booking " + booking.getId() + " is " + booking.getStatus().name().toLowerCase()
                            + ", only a COMPLETED booking can be reviewed");
        }

        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new ReviewAlreadyExistsException("Booking " + bookingId + " has already been reviewed");
        }

        // Derived from the booking, never taken from the request — the hotel
        // being reviewed must be the one the booking was actually for.
        Hotel hotel = booking.getRoom().getRoomType().getHotel();

        Review review = Review.builder()
                .user(currentUser.getUser())
                .hotel(hotel)
                .booking(booking)
                .rating(request.rating().shortValue())
                .comment(request.comment())
                .build();

        reviewRepository.save(review);
        return toResponse(review);
    }

    @Override
    public List<ReviewResponse> getByHotel(Long hotelId) {
        assertHotelExists(hotelId);
        return reviewRepository.findByHotelId(hotelId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public HotelRatingResponse getHotelRating(Long hotelId) {
        assertHotelExists(hotelId);
        Double average = reviewRepository.findAverageRatingByHotelId(hotelId);
        long count = reviewRepository.countByHotelId(hotelId);
        return new HotelRatingResponse(hotelId, average, count);
    }

    private void assertHotelExists(Long hotelId) {
        if (!hotelRepository.existsById(hotelId)) {
            throw new ResourceNotFoundException("Hotel not found with id: " + hotelId);
        }
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getHotel().getId(),
                review.getBooking().getId(),
                review.getUser().getId(),
                review.getRating().intValue(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
