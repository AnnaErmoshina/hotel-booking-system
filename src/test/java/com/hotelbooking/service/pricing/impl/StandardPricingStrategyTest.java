package com.hotelbooking.service.pricing.impl;

import com.hotelbooking.entity.Room;
import com.hotelbooking.entity.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StandardPricingStrategyTest {

    private final StandardPricingStrategy strategy = new StandardPricingStrategy();

    @Test
    void calculatePrice_isFlatRate_regardlessOfDayOfWeek() {
        Room room = Room.builder()
                .roomType(RoomType.builder().basePrice(BigDecimal.valueOf(100)).build())
                .build();

        BigDecimal price = strategy.calculatePrice(room, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 8));

        assertThat(price).isEqualByComparingTo("500");
    }
}
