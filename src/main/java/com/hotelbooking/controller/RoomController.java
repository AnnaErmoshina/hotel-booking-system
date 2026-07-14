package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateRoomRequest;
import com.hotelbooking.dto.response.RoomResponse;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.RoomService;
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
@Tag(name = "Rooms", description = "Physical rooms within a room type")
public class RoomController {

    private final RoomService roomService;

    @GetMapping("/api/room-types/{roomTypeId}/rooms")
    @Operation(summary = "List rooms of a room type")
    public ResponseEntity<List<RoomResponse>> getByRoomType(@PathVariable Long roomTypeId) {
        return ResponseEntity.ok(roomService.getByRoomType(roomTypeId));
    }

    @PostMapping("/api/room-types/{roomTypeId}/rooms")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN')")
    @Operation(summary = "Add a room to a room type (owner or ADMIN only)")
    public ResponseEntity<RoomResponse> create(
            @PathVariable Long roomTypeId,
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roomService.create(roomTypeId, request, currentUser));
    }
}
