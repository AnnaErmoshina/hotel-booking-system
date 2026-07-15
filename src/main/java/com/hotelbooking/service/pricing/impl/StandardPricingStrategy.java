package com.hotelbooking.service.pricing.impl;

import com.hotelbooking.entity.Room;
import com.hotelbooking.service.pricing.PricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Strategy pattern: flat rate, no seasonal variation — basePrice * nights.
 * This is the original pricing behaviour, kept as an explicit strategy so it
 * can still be selected (e.g. by qualifier) even though
 * {@link WeekendSurchargePricingStrategy} is now the default.
 */
@Component
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        return room.getRoomType().getBasePrice().multiply(BigDecimal.valueOf(nights));
    }
}
