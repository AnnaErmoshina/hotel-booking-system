package com.hotelbooking.controller;

import com.hotelbooking.dto.request.CreateBookingRequest;
import com.hotelbooking.dto.response.BookingResponse;
import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.security.UserPrincipal;
import com.hotelbooking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    private final UserPrincipal principal = new UserPrincipal(User.builder().id(1L).role(Role.USER).build());

    @Test
    void create_returns201() {
        BookingController controller = new BookingController(bookingService);
        CreateBookingRequest request = new CreateBookingRequest(1L, LocalDate.now(), LocalDate.now().plusDays(2));
        BookingResponse booking = new BookingResponse(1L, 1L, request.checkIn(), request.checkOut(), "PENDING", BigDecimal.TEN, null);
        when(bookingService.create(request, principal)).thenReturn(booking);

        var response = controller.create(request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo(booking);
    }

    @Test
    void getMyBookings_delegatesToService() {
        BookingController controller = new BookingController(bookingService);
        when(bookingService.getMyBookings(principal)).thenReturn(List.of());

        var response = controller.getMyBookings(principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(bookingService).getMyBookings(principal);
    }

    @Test
    void cancel_returns204() {
        BookingController controller = new BookingController(bookingService);

        var response = controller.cancel(1L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(bookingService).cancel(1L, principal);
    }

    @Test
    void complete_returns204() {
        BookingController controller = new BookingController(bookingService);

        var response = controller.complete(1L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(bookingService).complete(1L, principal);
    }
}
