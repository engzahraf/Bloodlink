package com.bloodlink.database.enums;

/**
 * Types of notifications delivered within BloodLink.
 */
public enum NotificationType {
    BLOOD_REQUEST,
    DONATION_UPDATE,
    STATUS_CHANGE,
    CHAT_MESSAGE,
    SYSTEM_ALERT;

    public static NotificationType fromString(String type) {
        if (type == null) return null;
        for (NotificationType n : values()) {
            if (n.name().equalsIgnoreCase(type.trim())) {
                return n;
            }
        }
        throw new IllegalArgumentException("Unknown NotificationType: " + type);
    }
}

