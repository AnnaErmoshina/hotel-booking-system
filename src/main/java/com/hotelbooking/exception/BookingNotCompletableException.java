package com.hotelbooking.exception;

/**
 * Thrown when a booking can't be marked COMPLETED: it isn't CONFIRMED yet,
 * or its check-out date hasn't arrived yet.
 */
public class BookingNotCompletableException extends RuntimeException {

    public BookingNotCompletableException(String message) {
        super(message);
    }
}
