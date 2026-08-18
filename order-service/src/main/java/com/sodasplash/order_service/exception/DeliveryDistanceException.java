package com.sodasplash.order_service.exception;


/**
 * Thrown when the delivery address geocodes to a location that
 * exceeds the configured maximum delivery distance from the store.
 */
public class DeliveryDistanceException extends RuntimeException {

    public DeliveryDistanceException(String message) {
        super(message);
    }
}
