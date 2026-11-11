package com.bloodlink.database.enums;

/**
 * Account status: ACTIVE or BLOCKED.
 */
public enum AccountStatus {
    ACTIVE,
    BLOCKED;

    public static AccountStatus fromString(String status) {
        if (status == null) return null;
        for (AccountStatus s : values()) {
            if (s.name().equalsIgnoreCase(status.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown AccountStatus: " + status);
    }
}

