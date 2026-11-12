package com.bloodlink.database.enums;

/**
 * Donor availability status.
 */
public enum AvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE;

    public static AvailabilityStatus fromString(String status) {
        if (status == null) return null;
        for (AvailabilityStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown AvailabilityStatus: " + status);
    }
}

