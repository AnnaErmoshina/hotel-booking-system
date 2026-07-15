package com.hotelbooking.service.pricing.impl;

import com.hotelbooking.entity.Room;
import com.hotelbooking.service.pricing.PricingStrategy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Strategy pattern: simple dynamic/seasonal pricing — each night is priced at
 * the room type's basePrice, except Friday and Saturday nights, which carry a
 * fixed surcharge (higher weekend demand). This is the default
 * ({@code @Primary}) strategy injected into BookingServiceImpl.
 * <p>
 * Deliberately simple (fixed multiplier, no external calendar of holidays or
 * per-hotel overrides) — the point demonstrated here is the pattern itself
 * (pluggable pricing behind one interface), not a full revenue-management
 * engine. See PROJECT_STATUS.md TODO 6.1 for what a fuller version could add.
 */
@Component
@Primary
public class WeekendSurchargePricingStrategy implements PricingStrategy {

    private static final BigDecimal WEEKEND_SURCHARGE_MULTIPLIER = BigDecimal.valueOf(1.2);

    @Override
    public BigDecimal calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal basePrice = room.getRoomType().getBasePrice();
        BigDecimal total = BigDecimal.ZERO;

        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            boolean isWeekendNight = night.getDayOfWeek() == DayOfWeek.FRIDAY
                    || night.getDayOfWeek() == DayOfWeek.SATURDAY;
            total = total.add(isWeekendNight
                    ? basePrice.multiply(WEEKEND_SURCHARGE_MULTIPLIER)
                    : basePrice);
        }

        return total;
    }
}
