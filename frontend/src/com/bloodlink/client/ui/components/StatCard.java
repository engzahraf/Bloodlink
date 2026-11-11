package com.bloodlink.client.ui.components;

import com.bloodlink.client.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Metric display card for BloodLink dashboards (Admin, Donor, Recipient).
 */
public class StatCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;
    private Color accentColor = ThemeManager.COLOR_PRIMARY;

    public StatCard(String title, String value, String subtitle, Color accentColor) {
        this.accentColor = accentColor != null ? accentColor : ThemeManager.COLOR_PRIMARY;

        setOpaque(false);
        setLayout(new BorderLayout(0, 6));
        setBorder(new EmptyBorder(16, 18, 16, 18));

        titleLabel = new JLabel(title);
        titleLabel.setFont(ThemeManager.FONT_SUBTITLE);
        titleLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        valueLabel = new JLabel(value);
        valueLabel.setFont(ThemeManager.FONT_STAT_NUMBER);
        valueLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        subtitleLabel = new JLabel(subtitle != null ? subtitle : "");
        subtitleLabel.setFont(ThemeManager.FONT_CAPTION);
        subtitleLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        JPanel contentPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        contentPanel.setOpaque(false);
        contentPanel.add(valueLabel);
        contentPanel.add(subtitleLabel);

        add(titleLabel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
        repaint();
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle != null ? subtitle : "");
        repaint();
    }

    public void setAccentColor(Color color) {
        this.accentColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.applyAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();
        int arc = 12;

        // Card background
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, arc, arc));

        // Border
        g2.setColor(ThemeManager.COLOR_BORDER);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, arc, arc));

        // Top accent bar
        g2.setColor(accentColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, 4, 4, 4));

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(220, 110);
    }
}
