package com.hotelbooking.service.pricing.impl;

import com.hotelbooking.entity.Hotel;
import com.hotelbooking.entity.Room;
import com.hotelbooking.entity.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WeekendSurchargePricingStrategyTest {

    private final WeekendSurchargePricingStrategy strategy = new WeekendSurchargePricingStrategy();

    private Room roomWithBasePrice(BigDecimal basePrice) {
        RoomType roomType = RoomType.builder().basePrice(basePrice).hotel(Hotel.builder().build()).build();
        return Room.builder().roomType(roomType).build();
    }

    @Test
    void calculatePrice_chargesBasePriceOnly_forAllWeekdayNights() {
        // Monday 2026-08-03 to Wednesday 2026-08-05 -> 2 weekday nights, no Fri/Sat
        Room room = roomWithBasePrice(BigDecimal.valueOf(100));

        BigDecimal price = strategy.calculatePrice(room, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 5));

        assertThat(price).isEqualByComparingTo("200");
    }

    @Test
    void calculatePrice_addsSurcharge_forFridayAndSaturdayNights() {
        // Friday 2026-08-07 to Sunday 2026-08-09 -> Fri + Sat nights, both surcharged
        Room room = roomWithBasePrice(BigDecimal.valueOf(100));

        BigDecimal price = strategy.calculatePrice(room, LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 9));

        // 100*1.2 + 100*1.2 = 240
        assertThat(price).isEqualByComparingTo("240.0");
    }

    @Test
    void calculatePrice_mixesWeekdayAndWeekendNights() {
        // Thursday 2026-08-06 to Monday 2026-08-10 -> Thu, Fri*, Sat*, Sun (4 nights, 2 surcharged)
        Room room = roomWithBasePrice(BigDecimal.valueOf(100));

        BigDecimal price = strategy.calculatePrice(room, LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 10));

        // 100 + 120 + 120 + 100 = 440
        assertThat(price).isEqualByComparingTo("440.0");
    }
}
