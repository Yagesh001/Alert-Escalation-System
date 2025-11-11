package com.movesync.alert.exception;

/**
 * Exception thrown when alert data is invalid
 */
public class InvalidAlertException extends RuntimeException {
    
    public InvalidAlertException(String message) {
        super(message);
    }

    public InvalidAlertException(String message, Throwable cause) {
        super(message, cause);
    }
}

