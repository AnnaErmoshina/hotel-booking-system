package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreatePaymentRequest;
import com.hotelbooking.dto.response.PaymentResponse;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Paying for bookings (requires authentication)")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Pay for a booking in full (owner or ADMIN); confirms the booking on success")
    public ResponseEntity<PaymentResponse> pay(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.pay(request, currentUser));
    }
}
