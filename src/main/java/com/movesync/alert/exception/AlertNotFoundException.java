package com.movesync.alert.exception;

/**
 * Exception thrown when an alert is not found
 */
public class AlertNotFoundException extends RuntimeException {
    
    public AlertNotFoundException(String message) {
        super(message);
    }
}

