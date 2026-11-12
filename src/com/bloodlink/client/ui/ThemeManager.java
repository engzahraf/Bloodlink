package com.bloodlink.client.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.lang.reflect.Method;

/**
 * Centralized healthcare theme system for BloodLink Swing desktop application.
 * Establishes typography, color palette, custom borders, and LookAndFeel initialization.
 */
public final class ThemeManager {

    // Healthcare Design Palette
    public static final Color COLOR_PRIMARY = new Color(0xC6, 0x28, 0x28);       // Crimson #C62828
    public static final Color COLOR_PRIMARY_DARK = new Color(0x8E, 0x00, 0x00);  // Dark Burgundy #8E0000
    public static final Color COLOR_PRIMARY_LIGHT = new Color(0xFF, 0xCD, 0xD2); // Soft Pink Accent #FFCDD2
    public static final Color COLOR_BACKGROUND = new Color(0xF8, 0xF9, 0xFA);    // Off-white / gray #F8F9FA
    public static final Color COLOR_SURFACE = new Color(0xFF, 0xFF, 0xFF);       // Card surface #FFFFFF
    public static final Color COLOR_BORDER = new Color(0xE0, 0xE0, 0xE0);        // Card border #E0E0E0
    public static final Color COLOR_BORDER_FOCUSED = new Color(0xC6, 0x28, 0x28);
    public static final Color COLOR_TEXT_PRIMARY = new Color(0x21, 0x21, 0x21);  // Text #212121
    public static final Color COLOR_TEXT_SECONDARY = new Color(0x75, 0x75, 0x75);// Secondary #757575
    public static final Color COLOR_TEXT_MUTED = new Color(0x9E, 0x9E, 0x9E);
    public static final Color COLOR_SUCCESS = new Color(0x2E, 0x7D, 0x32);       // Green #2E7D32
    public static final Color COLOR_WARNING = new Color(0xED, 0x6C, 0x02);       // Orange #ED6C02
    public static final Color COLOR_CRITICAL = new Color(0xD3, 0x2F, 0x2F);      // Deep Red #D32F2F
    public static final Color COLOR_MOCK_BANNER = new Color(0xFF, 0xF3, 0xCD);   // Amber Warning #FFF3CD
    public static final Color COLOR_MOCK_TEXT = new Color(0x85, 0x64, 0x04);

    // Font System
    private static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_HEADER = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 17);
    public static final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_CAPTION = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_TAGLINE = new Font(FONT_FAMILY, Font.ITALIC, 12);
    public static final Font FONT_STAT_NUMBER = new Font(FONT_FAMILY, Font.BOLD, 28);

    private ThemeManager() {
    }

    /**
     * Initializes modern Swing appearance. Attempts FlatLaf Light via reflection
     * so no mandatory build-time jar dependency is required, falling back cleanly to System L&F.
     */
    public static void initializeLookAndFeel() {
        try {
            // Attempt FlatLaf Light
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            Method setupMethod = flatLafClass.getMethod("setup");
            setupMethod.invoke(null);
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.width", 10);
        } catch (Throwable ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Fallback to cross-platform default
            }
        }

        // Apply consistent antialiased font rendering across all components
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        UIManager.put("Panel.background", COLOR_BACKGROUND);
        UIManager.put("OptionPane.background", COLOR_SURFACE);
        UIManager.put("OptionPane.messageForeground", COLOR_TEXT_PRIMARY);
    }

    public static Border createCardBorder() {
        return new CompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        );
    }

    public static Border createEmptyPadding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    public static void applyAntialiasing(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
    }
}
