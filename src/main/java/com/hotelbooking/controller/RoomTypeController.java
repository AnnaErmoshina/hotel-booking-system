package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateRoomTypeRequest;
import com.hotelbooking.dto.response.RoomTypeResponse;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.RoomTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Room Types", description = "Room categories within a hotel")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping("/api/hotels/{hotelId}/room-types")
    @Operation(summary = "List room types of a hotel")
    public ResponseEntity<List<RoomTypeResponse>> getByHotel(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomTypeService.getByHotel(hotelId));
    }

    @PostMapping("/api/hotels/{hotelId}/room-types")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN')")
    @Operation(summary = "Create a room type for a hotel (owner or ADMIN only)")
    public ResponseEntity<RoomTypeResponse> create(
            @PathVariable Long hotelId,
            @Valid @RequestBody CreateRoomTypeRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomTypeService.create(hotelId, request, currentUser));
    }
}
