package com.hotelbooking.service.pricing;

import com.hotelbooking.entity.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Strategy pattern: pluggable algorithms for computing a booking's total
 * price. BookingServiceImpl depends only on this interface, so pricing rules
 * can be swapped (or added) by adding a new @Component implementation and
 * marking it @Primary, without touching booking logic itself.
 *
 * See PROJECT_STATUS.md TODO 6.1/6.7 — this is the explicit GoF pattern the
 * course requirements ask for.
 */
public interface PricingStrategy {

    /**
     * @param room     the room being booked (carries its RoomType.basePrice)
     * @param checkIn  first night, inclusive
     * @param checkOut check-out date, exclusive (last night is checkOut minus 1 day)
     */
    BigDecimal calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);
}
