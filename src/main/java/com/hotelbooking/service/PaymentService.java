package com.hotelbooking.service;

import com.hotelbooking.dto.request.CreatePaymentRequest;
import com.hotelbooking.dto.response.PaymentResponse;
import com.hotelbooking.security.UserPrincipal;

public interface PaymentService {

    /**
     * Pays for a booking in full. On success the booking transitions
     * PENDING -> CONFIRMED (see BookingStatus). Only the booking's owner or
     * an ADMIN may pay for it.
     */
    PaymentResponse pay(CreatePaymentRequest request, UserPrincipal currentUser);
}
