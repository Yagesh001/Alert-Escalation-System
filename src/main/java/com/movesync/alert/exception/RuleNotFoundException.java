package com.movesync.alert.exception;

/**
 * Exception thrown when a rule is not found for a given alert type
 */
public class RuleNotFoundException extends RuntimeException {
    
    public RuleNotFoundException(String message) {
        super(message);
    }

    public RuleNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

