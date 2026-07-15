package com.hotelbooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "bookingId is required")
        Long bookingId,

        @NotBlank(message = "paymentMethod is required")
        String paymentMethod
) {
}
