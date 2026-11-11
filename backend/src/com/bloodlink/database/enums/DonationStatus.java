package com.bloodlink.database.enums;

/**
 * Status of a recorded donation.
 */
public enum DonationStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED;

    public static DonationStatus fromString(String status) {
        if (status == null) return null;
        for (DonationStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown DonationStatus: " + status);
    }
}

