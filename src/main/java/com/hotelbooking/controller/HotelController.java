package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateHotelRequest;
import com.hotelbooking.dto.response.HotelResponse;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Browsing and managing hotels")
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    @Operation(summary = "List hotels by city (paginated)")
    public ResponseEntity<Page<HotelResponse>> getByCity(
            @RequestParam String city,
            Pageable pageable
    ) {
        return ResponseEntity.ok(hotelService.getByCity(city, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a hotel by id")
    public ResponseEntity<HotelResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN')")
    @Operation(summary = "Create a new hotel (HOTEL_MANAGER or ADMIN)")
    public ResponseEntity<HotelResponse> create(
            @Valid @RequestBody CreateHotelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN')")
    @Operation(summary = "Update a hotel (owner or ADMIN only)")
    public ResponseEntity<HotelResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateHotelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(hotelService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN')")
    @Operation(summary = "Delete a hotel (owner or ADMIN only)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        hotelService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
