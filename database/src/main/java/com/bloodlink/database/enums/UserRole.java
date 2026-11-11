package com.bloodlink.database.enums;

/**
 * Enumeration representing user roles in BloodLink.
 */
public enum UserRole {
    ADMIN,
    DONOR,
    RECIPIENT;

    public static UserRole fromString(String role) {
        if (role == null) return null;
        for (UserRole r : values()) {
            if (r.name().equalsIgnoreCase(role.trim())) {
                return r;
            }
        }
        throw new IllegalArgumentException("Unknown UserRole: " + role);
    }
}

