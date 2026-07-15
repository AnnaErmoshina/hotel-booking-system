package com.hotelbooking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long roomId,
        LocalDate checkIn,
        LocalDate checkOut,
        String status,
        BigDecimal totalPrice,
        // Null unless the booking was cancelled — set by CancellationPolicy.
        BigDecimal cancellationFee
) {
}
