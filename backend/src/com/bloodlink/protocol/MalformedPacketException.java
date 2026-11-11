package com.bloodlink.protocol;

/**
 * Thrown when an incoming network frame violates the BloodLink wire format.
 */
public class MalformedPacketException extends Exception {

    private static final long serialVersionUID = 1L;

    public MalformedPacketException(String message) {
        super(message);
    }

    public MalformedPacketException(String message, Throwable cause) {
        super(message, cause);
    }
}
