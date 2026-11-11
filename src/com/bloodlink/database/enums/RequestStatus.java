package com.bloodlink.database.enums;

/**
 * Blood request lifecycle statuses.
 */
public enum RequestStatus {
    PENDING,
    MATCHED,
    ACCEPTED,
    COMPLETED,
    CANCELLED,
    REJECTED;

    public static RequestStatus fromString(String status) {
        if (status == null) return null;
        for (RequestStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown RequestStatus: " + status);
    }
}

