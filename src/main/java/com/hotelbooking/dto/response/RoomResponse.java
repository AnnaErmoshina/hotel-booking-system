package com.hotelbooking.dto.response;

public record RoomResponse(
        Long id,
        Long roomTypeId,
        String roomNumber,
        Integer floor,
        String status
) {
}
