package com.hotelbooking.service.cancellation.impl;

import com.hotelbooking.entity.Booking;
import com.hotelbooking.service.cancellation.CancellationPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Default {@link CancellationPolicy}: cancellation is free as long as it
 * happens at least {@code app.booking.free-cancellation-days} days before
 * check-in. Cancelling later than that charges a penalty of
 * {@code app.booking.late-cancellation-penalty-percent}% of the booking's
 * total price.
 * <p>
 * A booking that is cancelled on or after its own check-in date (e.g. a
 * no-show) is always charged the full penalty percentage — there is no
 * "too late to even charge a fee" case.
 */
@Component
@Primary
public class DeadlineCancellationPolicy implements CancellationPolicy {

    private final int freeCancellationDays;
    private final BigDecimal penaltyFraction;

    public DeadlineCancellationPolicy(
            @Value("${app.booking.free-cancellation-days:2}") int freeCancellationDays,
            @Value("${app.booking.late-cancellation-penalty-percent:50}") int lateCancellationPenaltyPercent
    ) {
        this.freeCancellationDays = freeCancellationDays;
        this.penaltyFraction = BigDecimal.valueOf(lateCancellationPenaltyPercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateFee(Booking booking, LocalDate now) {
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, booking.getCheckIn());

        if (daysUntilCheckIn >= freeCancellationDays) {
            return BigDecimal.ZERO;
        }

        return booking.getTotalPrice()
                .multiply(penaltyFraction)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
