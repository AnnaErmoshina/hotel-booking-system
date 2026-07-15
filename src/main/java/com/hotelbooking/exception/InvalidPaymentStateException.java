package com.hotelbooking.exception;

/**
 * Thrown when a payment can't be made: the booking is already paid for,
 * already cancelled/completed, or otherwise not in a payable state.
 */
public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
