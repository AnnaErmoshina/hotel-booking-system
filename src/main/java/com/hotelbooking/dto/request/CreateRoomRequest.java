package com.hotelbooking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRoomRequest(

        @NotBlank(message = "Room number is required")
        String roomNumber,

        Integer floor
) {
}
