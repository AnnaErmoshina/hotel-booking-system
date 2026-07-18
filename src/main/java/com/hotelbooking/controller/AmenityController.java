package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateAmenityRequest;
import com.hotelbooking.dto.response.AmenityResponse;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.AmenityService;
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
@Tag(name = "Amenities", description = "Hotel/room amenities catalog")
public class AmenityController {

    private final AmenityService amenityService;

    @GetMapping("/api/amenities")
    @Operation(summary = "List all amenities (public)")
    public ResponseEntity<List<AmenityResponse>> getAll() {
        return ResponseEntity.ok(amenityService.getAll());
    }

    @PostMapping("/api/amenities")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN')")
    @Operation(summary = "Create a new amenity (HOTEL_MANAGER or ADMIN only)")
    public ResponseEntity<AmenityResponse> create(@Valid @RequestBody CreateAmenityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(amenityService.create(request));
    }

    @GetMapping("/api/room-types/{roomTypeId}/amenities")
    @Operation(summary = "List amenities attached to a room type (public)")
    public ResponseEntity<List<AmenityResponse>> getByRoomType(@PathVariable Long roomTypeId) {
        return ResponseEntity.ok(amenityService.getByRoomType(roomTypeId));
    }

    @PostMapping("/api/room-types/{roomTypeId}/amenities/{amenityId}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN')")
    @Operation(summary = "Attach an existing amenity to a room type (owner of the hotel or ADMIN only)")
    public ResponseEntity<AmenityResponse> addToRoomType(
            @PathVariable Long roomTypeId,
            @PathVariable Long amenityId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(amenityService.addToRoomType(roomTypeId, amenityId, currentUser));
    }
}
