package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateBookingRequest;
import com.hotelbooking.dto.response.BookingResponse;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Creating and managing bookings (requires authentication)")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a booking for a room")
    public ResponseEntity<BookingResponse> create(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request, currentUser));
    }

    @GetMapping("/my")
    @Operation(summary = "List the current user's bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.ok(bookingService.getMyBookings(currentUser));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking (owner or ADMIN only)")
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        bookingService.cancel(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
