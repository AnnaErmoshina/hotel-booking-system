package com.hotelbooking.service.cancellation;

import com.hotelbooking.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Strategy pattern: decides how much (if anything) a user is charged for
 * cancelling a given booking. Mirrors {@link com.hotelbooking.service.pricing.PricingStrategy}
 * — {@code BookingServiceImpl} depends only on this interface, so alternative
 * policies (e.g. non-refundable rates, hotel-specific rules) can be added as
 * new {@code @Component} beans without touching the service layer.
 */
public interface CancellationPolicy {

    /**
     * @param booking the booking being cancelled (not yet mutated — status is
     *                still whatever it was before cancellation)
     * @param now     the date the cancellation is happening on (passed in,
     *                rather than read internally, to keep this easily testable)
     * @return the fee to charge, {@link BigDecimal#ZERO} if the cancellation
     * is free
     */
    BigDecimal calculateFee(Booking booking, LocalDate now);
}
