package com.hotelbooking.exception;

/**
 * Thrown when trying to review a booking that isn't COMPLETED yet — you can
 * only review a stay that actually happened.
 */
public class BookingNotReviewableException extends RuntimeException {

    public BookingNotReviewableException(String message) {
        super(message);
    }
}
