package com.bloodlink.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates standard ABO and Rh biological blood compatibility rules.
 * Determines eligible donor blood groups for any given recipient blood group.
 */
public final class BloodCompatibility {

    private static final Map<String, List<String>> COMPATIBILITY_TABLE = new HashMap<>();

    static {
        // Recipient Blood Group -> Compatible Donor Blood Groups (Red Blood Cells)
        COMPATIBILITY_TABLE.put("O_NEG", List.of("O_NEG"));
        COMPATIBILITY_TABLE.put("O_POS", List.of("O_NEG", "O_POS"));
        COMPATIBILITY_TABLE.put("A_NEG", List.of("O_NEG", "A_NEG"));
        COMPATIBILITY_TABLE.put("A_POS", List.of("O_NEG", "O_POS", "A_NEG", "A_POS"));
        COMPATIBILITY_TABLE.put("B_NEG", List.of("O_NEG", "B_NEG"));
        COMPATIBILITY_TABLE.put("B_POS", List.of("O_NEG", "O_POS", "B_NEG", "B_POS"));
        COMPATIBILITY_TABLE.put("AB_NEG", List.of("O_NEG", "A_NEG", "B_NEG", "AB_NEG"));
        COMPATIBILITY_TABLE.put("AB_POS", List.of("O_NEG", "O_POS", "A_NEG", "A_POS", "B_NEG", "B_POS", "AB_NEG", "AB_POS"));
    }

    private BloodCompatibility() {
        // Utility class
    }

    /**
     * Normalizes arbitrary user/database blood group strings (e.g. "O+", "o positive", "O_POS")
     * into the standard canonical token (e.g. "O_POS").
     */
    public static String normalize(String bloodGroup) {
        if (bloodGroup == null) {
            return "";
        }
        return bloodGroup.trim().toUpperCase()
                .replace(" ", "")
                .replace("POSITIVE", "_POS")
                .replace("NEGATIVE", "_NEG")
                .replace("+", "_POS")
                .replace("-", "_NEG");
    }

    /**
     * Checks if a blood group string maps to one of the 8 standard ABO/Rh blood groups.
     */
    public static boolean isValidBloodGroup(String bloodGroup) {
        if (bloodGroup == null || bloodGroup.trim().isEmpty()) {
            return false;
        }
        String normalized = normalize(bloodGroup);
        return COMPATIBILITY_TABLE.containsKey(normalized);
    }

    /**
     * Returns the set of all 8 standard normalized blood group tokens.
     */
    public static Set<String> getAllStandardGroups() {
        return Collections.unmodifiableSet(COMPATIBILITY_TABLE.keySet());
    }

    /**
     * Converts normalized token (e.g. "O_POS") to human-readable symbol ("O+").
     */
    public static String toSymbol(String normalized) {
        if (normalized == null) {
            return "";
        }
        return normalized.replace("_POS", "+").replace("_NEG", "-");
    }

    /**
     * Returns the list of all donor blood groups compatible with the given recipient group.
     */
    public static List<String> getCompatibleDonors(String recipientBloodGroup) {
        String normalized = normalize(recipientBloodGroup);
        List<String> donors = COMPATIBILITY_TABLE.get(normalized);
        if (donors != null) {
            return donors;
        }
        return Collections.emptyList();
    }

    /**
     * Checks if a specific donor blood group can donate to a specific recipient blood group.
     */
    public static boolean isCompatible(String donorBloodGroup, String recipientBloodGroup) {
        String normDonor = normalize(donorBloodGroup);
        List<String> allowed = getCompatibleDonors(recipientBloodGroup);
        return allowed.contains(normDonor);
    }
}
