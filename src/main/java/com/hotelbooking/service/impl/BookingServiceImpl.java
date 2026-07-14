package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreateBookingRequest;
import com.hotelbooking.dto.response.BookingResponse;
import com.hotelbooking.entity.Booking;
import com.hotelbooking.entity.Room;
import com.hotelbooking.entity.enums.BookingStatus;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.InvalidBookingDatesException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.exception.RoomNotAvailableException;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.RoomRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public BookingResponse create(CreateBookingRequest request, UserPrincipal currentUser) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new InvalidBookingDatesException("Check-out date must be after check-in date");
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.roomId()));

        boolean overlaps = bookingRepository.existsOverlappingBooking(
                room.getId(), request.checkIn(), request.checkOut(), BookingStatus.CANCELLED
        );

        if (overlaps) {
            throw new RoomNotAvailableException(
                    "Room " + room.getRoomNumber() + " is not available for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        BigDecimal totalPrice = room.getRoomType().getBasePrice().multiply(BigDecimal.valueOf(nights));

        Booking booking = Booking.builder()
                .user(currentUser.getUser())
                .room(room)
                .checkIn(request.checkIn())
                .checkOut(request.checkOut())
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .build();

        bookingRepository.save(booking);
        return toResponse(booking);
    }

    @Override
    public List<BookingResponse> getMyBookings(UserPrincipal currentUser) {
        return bookingRepository.findByUserId(currentUser.getUser().getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void cancel(Long bookingId, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        boolean isOwner = booking.getUser().getId().equals(currentUser.getUser().getId());
        boolean isAdmin = currentUser.getUser().getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You do not have permission to cancel this booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getRoom().getId(),
                booking.getCheckIn(),
                booking.getCheckOut(),
                booking.getStatus().name(),
                booking.getTotalPrice()
        );
    }
}
