package com.hotelbooking.exception;

/**
 * Thrown when trying to leave a second review for the same booking —
 * reviews.booking_id is unique 1:1 in the schema, one review per stay.
 */
public class ReviewAlreadyExistsException extends RuntimeException {

    public ReviewAlreadyExistsException(String message) {
        super(message);
    }
}
