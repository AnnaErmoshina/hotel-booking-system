package com.hotelbooking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long bookingId,
        BigDecimal amount,
        String status,
        String paymentMethod,
        LocalDateTime paidAt
) {
}
