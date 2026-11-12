package com.bloodlink.client.ui.components;

import com.bloodlink.client.ui.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern, styled Swing button with rounded corners, smooth hover effects,
 * and semantic color schemes (Primary, Secondary, Danger, Outline).
 */
public class ModernButton extends JButton {

    public enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        DANGER,
        OUTLINE
    }

    private ButtonStyle style = ButtonStyle.PRIMARY;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 10;

    public ModernButton(String text) {
        this(text, ButtonStyle.PRIMARY);
    }

    public ModernButton(String text, ButtonStyle style) {
        super(text);
        this.style = style;
        initButton();
    }

    private void initButton() {
        setFont(ThemeManager.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(10, 20, 10, 20));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setButtonStyle(ButtonStyle style) {
        this.style = style;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.applyAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();

        Color bgColor;
        Color fgColor;
        Color borderColor = null;

        switch (style) {
            case PRIMARY:
                bgColor = isPressed ? ThemeManager.COLOR_PRIMARY_DARK :
                        (isHovered ? ThemeManager.COLOR_PRIMARY_DARK : ThemeManager.COLOR_PRIMARY);
                fgColor = Color.WHITE;
                break;
            case SECONDARY:
                bgColor = isPressed ? new Color(0xDC, 0xE0, 0xE4) :
                        (isHovered ? new Color(0xEA, 0xEE, 0xF2) : new Color(0xF1, 0xF3, 0xF5));
                fgColor = ThemeManager.COLOR_TEXT_PRIMARY;
                borderColor = ThemeManager.COLOR_BORDER;
                break;
            case DANGER:
                bgColor = isPressed ? new Color(0xB7, 0x1C, 0x1C) :
                        (isHovered ? new Color(0xC6, 0x28, 0x28) : ThemeManager.COLOR_CRITICAL);
                fgColor = Color.WHITE;
                break;
            case OUTLINE:
            default:
                bgColor = isPressed ? new Color(0xFF, 0xCD, 0xD2) :
                        (isHovered ? new Color(0xFD, 0xEB, 0xEC) : Color.WHITE);
                fgColor = ThemeManager.COLOR_PRIMARY;
                borderColor = isHovered ? ThemeManager.COLOR_PRIMARY_DARK : ThemeManager.COLOR_PRIMARY;
                break;
        }

        if (!isEnabled()) {
            bgColor = new Color(0xEE, 0xEE, 0xEE);
            fgColor = ThemeManager.COLOR_TEXT_MUTED;
            borderColor = ThemeManager.COLOR_BORDER;
        }

        // Draw background
        g2.setColor(bgColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        // Draw optional border
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, cornerRadius, cornerRadius));
        }

        // Draw text
        g2.setColor(fgColor);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width + 24, 90), Math.max(d.height + 12, 38));
    }
}
