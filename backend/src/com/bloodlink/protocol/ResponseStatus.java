package com.bloodlink.protocol;

/**
 * Standard status codes returned by the BloodLink server in response packets.
 */
public enum ResponseStatus {
    SUCCESS,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL_ERROR
}
