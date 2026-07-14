package com.hotelbooking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateHotelRequest(

        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotBlank(message = "Address is required")
        String address,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Country is required")
        String country
) {
}
