package com.bloodlink.client.ui.components;

import com.bloodlink.client.ui.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern status pill / badge displaying states such as URGENT, AVAILABLE, PENDING, OPEN.
 */
public class StatusBadge extends JComponent {

    public enum StatusType {
        SUCCESS,  // Green (e.g. AVAILABLE, FULFILLED, CONNECTED)
        WARNING,  // Orange (e.g. PENDING, IN_PROGRESS)
        DANGER,   // Crimson/Red (e.g. URGENT, CRITICAL, DISCONNECTED)
        NEUTRAL   // Gray (e.g. CLOSED, OFFLINE, UNAVAILABLE)
    }

    private String text;
    private StatusType type;

    public StatusBadge(String text, StatusType type) {
        this.text = text != null ? text.toUpperCase() : "";
        this.type = type != null ? type : StatusType.NEUTRAL;
        setFont(ThemeManager.FONT_CAPTION);
    }

    public void setBadge(String text, StatusType type) {
        this.text = text != null ? text.toUpperCase() : "";
        this.type = type != null ? type : StatusType.NEUTRAL;
        revalidate();
        repaint();
    }

    public static StatusType resolveStatusType(String statusOrUrgency) {
        if (statusOrUrgency == null) return StatusType.NEUTRAL;
        String s = statusOrUrgency.trim().toUpperCase();
        switch (s) {
            case "AVAILABLE":
            case "OPEN":
            case "FULFILLED":
            case "CONNECTED":
            case "SUCCESS":
            case "NORMAL":
                return StatusType.SUCCESS;
            case "PENDING":
            case "IN_PROGRESS":
            case "CONNECTING":
            case "RECONNECTING":
                return StatusType.WARNING;
            case "URGENT":
            case "CRITICAL":
            case "DISCONNECTED":
            case "FAILED":
            case "ERROR":
                return StatusType.DANGER;
            default:
                return StatusType.NEUTRAL;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.applyAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();

        Color bgColor;
        Color fgColor;

        switch (type) {
            case SUCCESS:
                bgColor = new Color(0xE8, 0xF5, 0xE9);
                fgColor = ThemeManager.COLOR_SUCCESS;
                break;
            case WARNING:
                bgColor = new Color(0xFF, 0xF3, 0xE0);
                fgColor = ThemeManager.COLOR_WARNING;
                break;
            case DANGER:
                bgColor = new Color(0xFF, 0xEB, 0xEE);
                fgColor = ThemeManager.COLOR_CRITICAL;
                break;
            case NEUTRAL:
            default:
                bgColor = new Color(0xEE, 0xF0, 0xF2);
                fgColor = ThemeManager.COLOR_TEXT_SECONDARY;
                break;
        }

        // Pill background
        g2.setColor(bgColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, height, height));

        // Status indicator dot
        int dotDiameter = 6;
        int dotY = (height - dotDiameter) / 2;
        int dotX = 8;
        g2.setColor(fgColor);
        g2.fillOval(dotX, dotY, dotDiameter, dotDiameter);

        // Text
        g2.setFont(ThemeManager.FONT_CAPTION);
        FontMetrics fm = g2.getFontMetrics();
        int textX = dotX + dotDiameter + 5;
        int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(text, textX, textY);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(ThemeManager.FONT_CAPTION);
        int textWidth = fm.stringWidth(text);
        return new Dimension(textWidth + 26, 22);
    }
}
