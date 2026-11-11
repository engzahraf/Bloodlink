package com.bloodlink.database.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enumeration representing human blood groups and compatibility.
 */
public enum BloodGroup {
    A_POSITIVE("A+"),
    A_NEGATIVE("A-"),
    B_POSITIVE("B+"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    private final String symbol;

    BloodGroup(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static BloodGroup fromString(String text) {
        if (text == null) return null;
        String clean = text.trim();
        for (BloodGroup bg : values()) {
            if (bg.name().equalsIgnoreCase(clean) || bg.symbol.equalsIgnoreCase(clean)) {
                return bg;
            }
        }
        throw new IllegalArgumentException("Unknown BloodGroup: " + text);
    }

    /**
     * Returns the list of blood groups that this blood group can safely receive from.
     */
    public List<BloodGroup> getCompatibleDonorGroups() {
        switch (this) {
            case A_POSITIVE:
                return Arrays.asList(A_POSITIVE, A_NEGATIVE, O_POSITIVE, O_NEGATIVE);
            case A_NEGATIVE:
                return Arrays.asList(A_NEGATIVE, O_NEGATIVE);
            case B_POSITIVE:
                return Arrays.asList(B_POSITIVE, B_NEGATIVE, O_POSITIVE, O_NEGATIVE);
            case B_NEGATIVE:
                return Arrays.asList(B_NEGATIVE, O_NEGATIVE);
            case AB_POSITIVE:
                return Arrays.asList(values()); // Universal recipient
            case AB_NEGATIVE:
                return Arrays.asList(AB_NEGATIVE, A_NEGATIVE, B_NEGATIVE, O_NEGATIVE);
            case O_POSITIVE:
                return Arrays.asList(O_POSITIVE, O_NEGATIVE);
            case O_NEGATIVE:
                return Collections.singletonList(O_NEGATIVE);
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        return symbol;
    }
}

