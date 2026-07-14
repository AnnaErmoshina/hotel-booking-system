package com.hotelbooking.exception;

public class InvalidBookingDatesException extends RuntimeException {

    public InvalidBookingDatesException(String message) {
        super(message);
    }
}
