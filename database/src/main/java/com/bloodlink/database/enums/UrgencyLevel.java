package com.bloodlink.database.enums;

/**
 * Urgency levels for blood requests.
 */
public enum UrgencyLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static UrgencyLevel fromString(String level) {
        if (level == null) return null;
        for (UrgencyLevel u : values()) {
            if (u.name().equalsIgnoreCase(level.trim())) {
                return u;
            }
        }
        throw new IllegalArgumentException("Unknown UrgencyLevel: " + level);
    }
}

