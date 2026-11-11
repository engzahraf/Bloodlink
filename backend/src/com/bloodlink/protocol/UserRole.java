package com.bloodlink.protocol;

/**
 * Standard user roles defined in the BloodLink system.
 */
public final class UserRole {

    public static final String DONOR = "DONOR";
    public static final String RECIPIENT = "RECIPIENT";
    public static final String HOSPITAL = "HOSPITAL";
    public static final String ADMIN = "ADMIN";

    private UserRole() {
        // Constants class
    }

    /**
     * Checks if a role string is a recognized BloodLink role.
     */
    public static boolean isValid(String role) {
        if (role == null) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return DONOR.equals(r) || RECIPIENT.equals(r) || HOSPITAL.equals(r) || ADMIN.equals(r);
    }

    /**
     * Checks if a role is allowed for public self-registration.
     * Ordinary users cannot register themselves as ADMIN.
     */
    public static boolean isSelfRegistrable(String role) {
        if (role == null) {
            return false;
        }
        String r = role.trim().toUpperCase();
        return DONOR.equals(r) || RECIPIENT.equals(r) || HOSPITAL.equals(r);
    }
}
