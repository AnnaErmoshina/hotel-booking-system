package com.hotelbooking.service.impl;

import com.hotelbooking.dto.request.CreatePaymentRequest;
import com.hotelbooking.dto.response.PaymentResponse;
import com.hotelbooking.entity.Booking;
import com.hotelbooking.entity.Payment;
import com.hotelbooking.entity.enums.BookingStatus;
import com.hotelbooking.entity.enums.PaymentStatus;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.exception.ForbiddenOperationException;
import com.hotelbooking.exception.InvalidPaymentStateException;
import com.hotelbooking.exception.ResourceNotFoundException;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.PaymentRepository;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public PaymentResponse pay(CreatePaymentRequest request, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + request.bookingId()));

        boolean isOwner = booking.getUser().getId().equals(currentUser.getUser().getId());
        boolean isAdmin = currentUser.getUser().getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenOperationException("You do not have permission to pay for this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidPaymentStateException(
                    "Booking " + booking.getId() + " is " + booking.getStatus().name().toLowerCase()
                            + " and cannot be paid for");
        }

        // 1:1 with booking (unique booking_id column) — a second payment
        // attempt for the same booking is a conflict, not a new row.
        if (paymentRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new InvalidPaymentStateException(
                    "Booking " + booking.getId() + " already has a payment");
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getTotalPrice())
                .status(PaymentStatus.PAID)
                .paymentMethod(request.paymentMethod())
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        // Successful payment is what moves a booking from PENDING to
        // CONFIRMED — see PROJECT_STATUS.md TODO 6.5.
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getAmount(),
                payment.getStatus().name(),
                payment.getPaymentMethod(),
                payment.getPaidAt()
        );
    }
}
