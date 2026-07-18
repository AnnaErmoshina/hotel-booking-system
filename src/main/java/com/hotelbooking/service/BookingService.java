package com.hotelbooking.service;

import com.hotelbooking.dto.request.CreateBookingRequest;
import com.hotelbooking.dto.response.BookingResponse;
import com.hotelbooking.security.UserPrincipal;

import java.util.List;

public interface BookingService {

    BookingResponse create(CreateBookingRequest request, UserPrincipal currentUser);

    List<BookingResponse> getMyBookings(UserPrincipal currentUser);

    void cancel(Long bookingId, UserPrincipal currentUser);

    void complete(Long bookingId, UserPrincipal currentUser);
}
