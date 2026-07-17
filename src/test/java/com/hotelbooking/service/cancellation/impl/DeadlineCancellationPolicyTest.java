package com.hotelbooking.service.cancellation.impl;

import com.hotelbooking.entity.Booking;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DeadlineCancellationPolicyTest {

    // free-cancellation-days=2, penalty=50%, matching application.yml defaults
    private final DeadlineCancellationPolicy policy = new DeadlineCancellationPolicy(2, 50);

    @Test
    void calculateFee_isZero_whenCancelledWellBeforeDeadline() {
        LocalDate today = LocalDate.of(2026, 8, 1);
        Booking booking = Booking.builder()
                .checkIn(LocalDate.of(2026, 8, 10))
                .totalPrice(BigDecimal.valueOf(200))
                .build();

        BigDecimal fee = policy.calculateFee(booking, today);

        assertThat(fee).isEqualByComparingTo("0");
    }

    @Test
    void calculateFee_isZero_whenCancelledExactlyAtDeadline() {
        LocalDate today = LocalDate.of(2026, 8, 8);
        Booking booking = Booking.builder()
                .checkIn(LocalDate.of(2026, 8, 10)) // exactly 2 days out
                .totalPrice(BigDecimal.valueOf(200))
                .build();

        BigDecimal fee = policy.calculateFee(booking, today);

        assertThat(fee).isEqualByComparingTo("0");
    }

    @Test
    void calculateFee_chargesPenaltyPercentage_whenCancelledPastDeadline() {
        LocalDate today = LocalDate.of(2026, 8, 9); // 1 day before check-in
        Booking booking = Booking.builder()
                .checkIn(LocalDate.of(2026, 8, 10))
                .totalPrice(BigDecimal.valueOf(200))
                .build();

        BigDecimal fee = policy.calculateFee(booking, today);

        assertThat(fee).isEqualByComparingTo("100.00");
    }

    @Test
    void calculateFee_chargesPenalty_onOrAfterCheckInDate() {
        LocalDate today = LocalDate.of(2026, 8, 10); // same-day, no-show style
        Booking booking = Booking.builder()
                .checkIn(LocalDate.of(2026, 8, 10))
                .totalPrice(BigDecimal.valueOf(200))
                .build();

        BigDecimal fee = policy.calculateFee(booking, today);

        assertThat(fee).isEqualByComparingTo("100.00");
    }
}
