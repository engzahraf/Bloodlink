package com.bloodlink.client.ui.components;

import com.bloodlink.client.models.User;
import com.bloodlink.client.ui.NavigationManager;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Top brand and navigation header bar used across all BloodLink dashboards.
 */
public class HeaderPanel extends JPanel {

    private final JLabel userGreetingLabel;
    private final StatusBadge connectionBadge;
    private final StatusBadge modeBadge;
    private final StatusBadge roleBadge;
    private final ModernButton logoutButton;
    private final ModernButton notifButton;

    public HeaderPanel(NavigationManager navigationManager) {
        setOpaque(true);
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 24, 12, 24));

        // Left: Brand Logo & Tagline
        JPanel brandPanel = new JPanel();
        brandPanel.setOpaque(false);
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);

        JLabel brandIcon = new JLabel("🩸");
        brandIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        JLabel brandTitle = new JLabel("BloodLink");
        brandTitle.setFont(ThemeManager.FONT_HEADER);
        brandTitle.setForeground(ThemeManager.COLOR_PRIMARY);

        roleBadge = new StatusBadge("DONOR", StatusBadge.StatusType.NEUTRAL);

        titleRow.add(brandIcon);
        titleRow.add(brandTitle);
        titleRow.add(roleBadge);

        JLabel tagline = new JLabel("Connecting Lives Through Blood Donation");
        tagline.setFont(ThemeManager.FONT_TAGLINE);
        tagline.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);
        tagline.setBorder(new EmptyBorder(2, 36, 0, 0));

        brandPanel.add(titleRow);
        brandPanel.add(tagline);

        // Right: User info, Mode indicator, Notifications & Logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 4));
        rightPanel.setOpaque(false);

        modeBadge = new StatusBadge("LIVE MODE", StatusBadge.StatusType.SUCCESS);
        connectionBadge = new StatusBadge("CONNECTED", StatusBadge.StatusType.SUCCESS);

        userGreetingLabel = new JLabel("Welcome");
        userGreetingLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        userGreetingLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        notifButton = new ModernButton("🔔 Notifications", ModernButton.ButtonStyle.SECONDARY);
        notifButton.setFont(ThemeManager.FONT_CAPTION);
        notifButton.addActionListener(e -> {
            if (navigationManager != null) {
                navigationManager.showNotifications();
            }
        });

        logoutButton = new ModernButton("Logout", ModernButton.ButtonStyle.OUTLINE);
        logoutButton.setFont(ThemeManager.FONT_CAPTION);
        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    HeaderPanel.this,
                    "Are you sure you want to log out of BloodLink?",
                    "Confirm Logout",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION && navigationManager != null) {
                navigationManager.logout();
            }
        });

        rightPanel.add(modeBadge);
        rightPanel.add(connectionBadge);
        rightPanel.add(userGreetingLabel);
        rightPanel.add(notifButton);
        rightPanel.add(logoutButton);

        add(brandPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        refreshState();
    }

    public void refreshState() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            userGreetingLabel.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
            roleBadge.setBadge(user.getRole(), StatusBadge.StatusType.SUCCESS);
        } else {
            userGreetingLabel.setText("Guest");
            roleBadge.setBadge("GUEST", StatusBadge.StatusType.NEUTRAL);
        }

        boolean isMock = SessionManager.getInstance().isMockMode();
        if (isMock) {
            modeBadge.setBadge("MOCK MODE", StatusBadge.StatusType.WARNING);
            connectionBadge.setBadge("OFFLINE", StatusBadge.StatusType.NEUTRAL);
        } else {
            modeBadge.setBadge("LIVE TCP", StatusBadge.StatusType.SUCCESS);
            connectionBadge.setBadge("ONLINE", StatusBadge.StatusType.SUCCESS);
        }

        revalidate();
        repaint();
    }

    public void setConnectionStatus(boolean connected) {
        if (SessionManager.getInstance().isMockMode()) {
            connectionBadge.setBadge("MOCK", StatusBadge.StatusType.WARNING);
        } else if (connected) {
            connectionBadge.setBadge("CONNECTED", StatusBadge.StatusType.SUCCESS);
        } else {
            connectionBadge.setBadge("DISCONNECTED", StatusBadge.StatusType.DANGER);
        }
    }

    public void setUnreadNotificationCount(int count) {
        if (count > 0) {
            notifButton.setText("🔔 Notifications (" + count + ")");
            notifButton.setButtonStyle(ModernButton.ButtonStyle.PRIMARY);
        } else {
            notifButton.setText("🔔 Notifications");
            notifButton.setButtonStyle(ModernButton.ButtonStyle.SECONDARY);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw bottom divider line
        g.setColor(ThemeManager.COLOR_BORDER);
        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
    }
}
