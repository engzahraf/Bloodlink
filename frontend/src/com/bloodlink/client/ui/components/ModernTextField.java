package com.bloodlink.client.ui.components;

import com.bloodlink.client.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern styled text field with rounded corners, placeholder support,
 * and elegant focus ring animations.
 */
public class ModernTextField extends JTextField {

    private String placeholder = "";
    private boolean isFocused = false;
    private int cornerRadius = 8;

    public ModernTextField() {
        this("", "");
    }

    public ModernTextField(int columns) {
        this("", "", columns);
    }

    public ModernTextField(String text, String placeholder) {
        this(text, placeholder, 0);
    }

    public ModernTextField(String text, String placeholder, int columns) {
        super(text, columns);
        this.placeholder = placeholder != null ? placeholder : "";
        initField();
    }

    private void initField() {
        setFont(ThemeManager.FONT_BODY);
        setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        setBackground(Color.WHITE);
        setCaretColor(ThemeManager.COLOR_PRIMARY);
        setOpaque(false);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.applyAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();

        // Background
        g2.setColor(isEnabled() ? Color.WHITE : new Color(0xF8, 0xF9, 0xFA));
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        // Text & placeholder
        super.paintComponent(g);

        if (getText().isEmpty() && !placeholder.isEmpty() && !isFocused) {
            g2.setColor(ThemeManager.COLOR_TEXT_MUTED);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int y = (height + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(placeholder, getInsets().left, y);
        }

        // Focus / Border
        if (isFocused) {
            g2.setColor(ThemeManager.COLOR_PRIMARY);
            g2.setStroke(new BasicStroke(1.8f));
        } else {
            g2.setColor(ThemeManager.COLOR_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, cornerRadius, cornerRadius));

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 140), Math.max(d.height, 36));
    }
}
