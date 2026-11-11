package com.bloodlink.client.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reusable validation utilities for input forms across BloodLink screens.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9]{7,15}$"
    );

    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_.-]{3,30}$"
    );

    private static final Set<String> VALID_BLOOD_GROUPS = new HashSet<>(Arrays.asList(
            "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-",
            "O_POS", "O_NEG", "A_POS", "A_NEG", "B_POS", "B_NEG", "AB_POS", "AB_NEG",
            "O_POSITIVE", "O_NEGATIVE", "A_POSITIVE", "A_NEGATIVE", "B_POSITIVE", "B_NEGATIVE", "AB_POSITIVE", "AB_NEGATIVE"
    ));

    private ValidationUtils() {
    }

    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (!isNotEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (!isNotEmpty(phone)) return false;
        String sanitized = phone.trim().replaceAll("[\\s-]", "");
        return PHONE_PATTERN.matcher(sanitized).matches();
    }

    public static boolean isValidUsername(String username) {
        if (!isNotEmpty(username)) return false;
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    public static boolean isValidPassword(char[] password) {
        return password != null && password.length >= 6;
    }

    public static boolean isValidBloodGroup(String bg) {
        if (!isNotEmpty(bg)) return false;
        String normalized = bg.trim().toUpperCase();
        return VALID_BLOOD_GROUPS.contains(normalized);
    }

    /**
     * Normalizes any accepted blood group string to wire token format expected by backend:
     * e.g. "O+" -> "O_POS", "AB-" -> "AB_NEG".
     */
    public static String toCanonicalWireBloodGroup(String bg) {
        if (bg == null) return "O_POS";
        String s = bg.trim().toUpperCase();
        switch (s) {
            case "O+":
            case "O_POSITIVE":
            case "O_POS":
                return "O_POS";
            case "O-":
            case "O_NEGATIVE":
            case "O_NEG":
                return "O_NEG";
            case "A+":
            case "A_POSITIVE":
            case "A_POS":
                return "A_POS";
            case "A-":
            case "A_NEGATIVE":
            case "A_NEG":
                return "A_NEG";
            case "B+":
            case "B_POSITIVE":
            case "B_POS":
                return "B_POS";
            case "B-":
            case "B_NEGATIVE":
            case "B_NEG":
                return "B_NEG";
            case "AB+":
            case "AB_POSITIVE":
            case "AB_POS":
                return "AB_POS";
            case "AB-":
            case "AB_NEGATIVE":
            case "AB_NEG":
                return "AB_NEG";
            default:
                return s;
        }
    }

    /**
     * Converts canonical wire token to human-friendly UI symbol:
     * "O_POS" -> "O+", "AB_NEG" -> "AB-".
     */
    public static String toDisplayBloodGroup(String bg) {
        if (bg == null) return "O+";
        String s = bg.trim().toUpperCase();
        switch (s) {
            case "O_POS":
            case "O_POSITIVE":
                return "O+";
            case "O_NEG":
            case "O_NEGATIVE":
                return "O-";
            case "A_POS":
            case "A_POSITIVE":
                return "A+";
            case "A_NEG":
            case "A_NEGATIVE":
                return "A-";
            case "B_POS":
            case "B_POSITIVE":
                return "B+";
            case "B_NEG":
            case "B_NEGATIVE":
                return "B-";
            case "AB_POS":
            case "AB_POSITIVE":
                return "AB+";
            case "AB_NEG":
            case "AB_NEGATIVE":
                return "AB-";
            default:
                return s;
        }
    }

    public static boolean isValidUnits(String units) {
        if (!isNotEmpty(units)) return false;
        try {
            int val = Integer.parseInt(units.trim());
            return val > 0 && val <= 50;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
