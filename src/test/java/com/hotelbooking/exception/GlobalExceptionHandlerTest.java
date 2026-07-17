package com.hotelbooking.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAccessDenied_returns403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("denied"));
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void handleResourceNotFound_returns404WithMessage() {
        var response = handler.handleResourceNotFound(new ResourceNotFoundException("not found"));
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("not found");
    }

    @Test
    void handleRoomNotAvailable_returns409() {
        var response = handler.handleRoomNotAvailable(new RoomNotAvailableException("taken"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleInvalidBookingDates_returns400() {
        var response = handler.handleInvalidBookingDates(new InvalidBookingDatesException("bad dates"));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void handleForbiddenOperation_returns403() {
        var response = handler.handleForbiddenOperation(new ForbiddenOperationException("no"));
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void handleBookingAlreadyCancelled_returns409() {
        var response = handler.handleBookingAlreadyCancelled(new BookingAlreadyCancelledException("already"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleInvalidPaymentState_returns409() {
        var response = handler.handleInvalidPaymentState(new InvalidPaymentStateException("paid"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleBookingNotCompletable_returns409() {
        var response = handler.handleBookingNotCompletable(new BookingNotCompletableException("too soon"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleEmailAlreadyExists_returns409() {
        var response = handler.handleEmailAlreadyExists(new EmailAlreadyExistsException("taken"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleAmenityAlreadyExists_returns409() {
        var response = handler.handleAmenityAlreadyExists(new AmenityAlreadyExistsException("taken"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleReviewAlreadyExists_returns409() {
        var response = handler.handleReviewAlreadyExists(new ReviewAlreadyExistsException("taken"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleBookingNotReviewable_returns409() {
        var response = handler.handleBookingNotReviewable(new BookingNotReviewableException("not yet"));
        assertThat(response.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void handleBadCredentials_returns401WithGenericMessage() {
        var response = handler.handleBadCredentials(new BadCredentialsException("wrong password"));
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("Invalid email or password");
    }

    @Test
    void handleValidation_joinsFieldErrorsIntoMessage() {
        FieldError fieldError = new FieldError("request", "email", "must be valid");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        var response = handler.handleValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("email: must be valid");
    }

    @Test
    void handleGeneric_returns500WithoutLeakingExceptionDetails() {
        var response = handler.handleGeneric(new RuntimeException("some internal detail"));
        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().message()).isEqualTo("Unexpected error occurred");
    }
}
