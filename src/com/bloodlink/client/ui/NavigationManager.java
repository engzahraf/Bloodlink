package com.bloodlink.client.ui;

import com.bloodlink.client.models.User;
import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.screens.*;
import com.bloodlink.client.utils.SessionManager;

import javax.swing.*;
import java.awt.*;

/**
 * Centralized screen router and navigation manager for BloodLink.
 * Handles transitioning between Login, Registration, and Role-specific Dashboards.
 */
public class NavigationManager {

    private JFrame activeFrame;

    public NavigationManager() {
    }

    public void showLogin() {
        closeActiveFrame();
        LoginFrame loginFrame = new LoginFrame(this);
        setActiveFrame(loginFrame);
        loginFrame.setVisible(true);
    }

    public void showRegister() {
        closeActiveFrame();
        RegisterFrame registerFrame = new RegisterFrame(this);
        setActiveFrame(registerFrame);
        registerFrame.setVisible(true);
    }

    public void navigateToDashboard(User user) {
        if (user == null) {
            showLogin();
            return;
        }

        closeActiveFrame();
        SessionManager.getInstance().setCurrentUser(user);

        JFrame dashboardFrame;
        String role = user.getRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            dashboardFrame = new AdminDashboard(this);
        } else if ("RECIPIENT".equalsIgnoreCase(role)) {
            dashboardFrame = new RecipientDashboard(this);
        } else {
            // Default: DONOR
            dashboardFrame = new DonorDashboard(this);
        }

        setActiveFrame(dashboardFrame);
        dashboardFrame.setVisible(true);
    }

    public void showCreateRequestDialog(JFrame parent, Runnable onSuccess) {
        JDialog dialog = new JDialog(parent, "Create Blood Request", true);
        BloodRequestPanel panel = new BloodRequestPanel(dialog, onSuccess);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setSize(560, 580);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public void showDonorProfileDialog(JFrame parent, Runnable onUpdated) {
        JDialog dialog = new JDialog(parent, "Donor Profile & Availability", true);
        DonorProfilePanel panel = new DonorProfilePanel(dialog, onUpdated);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setSize(540, 560);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public void showDonorSearchDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Search Eligible Blood Donors", true);
        DonorSearchPanel panel = new DonorSearchPanel(this, dialog);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setSize(860, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public void showNotifications() {
        JDialog dialog = new JDialog(activeFrame, "Notifications & Urgent Alerts", true);
        NotificationPanel panel = new NotificationPanel(dialog);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setSize(640, 520);
        dialog.setLocationRelativeTo(activeFrame);
        dialog.setVisible(true);
    }

    public void showMessagingDialog(JFrame parent, String targetUserId, String targetUserName) {
        JDialog dialog = new JDialog(parent, "Direct Messaging - " + (targetUserName != null ? targetUserName : "Chat"), true);
        MessagingPanel panel = new MessagingPanel(targetUserId, targetUserName);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setSize(760, 560);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public void showViewDonorProfileDialog(JFrame parent, String donorId, String donorName, String bloodGroup,
                                           String city, String phone, String email, String availability, String lastDonation) {
        JDialog dialog = new JDialog(parent, "Donor Profile - " + (donorName != null ? donorName : "Donor"), true);
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel(donorName != null ? donorName : "Donor Profile");
        title.setFont(ThemeManager.FONT_HEADER);
        title.setForeground(ThemeManager.COLOR_PRIMARY);
        JLabel sub = new JLabel("Blood Donor Profile • Blood Group: " + com.bloodlink.client.utils.ValidationUtils.toDisplayBloodGroup(bloodGroup));
        sub.setFont(ThemeManager.FONT_BODY_BOLD);
        sub.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);
        header.add(title);
        header.add(sub);

        JPanel details = new JPanel(new GridLayout(6, 2, 8, 10));
        details.setOpaque(true);
        details.setBackground(new Color(0xFA, 0xFA, 0xFA));
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        details.add(new JLabel("Donor ID:"));
        details.add(new JLabel(donorId != null ? donorId : "-"));
        details.add(new JLabel("Blood Group:"));
        details.add(new JLabel(com.bloodlink.client.utils.ValidationUtils.toDisplayBloodGroup(bloodGroup)));
        details.add(new JLabel("Location / City:"));
        details.add(new JLabel(city != null && !city.isEmpty() ? city : "Not specified"));
        details.add(new JLabel("Contact Phone:"));
        details.add(new JLabel(phone != null && !phone.isEmpty() ? phone : "Not specified"));
        details.add(new JLabel("Email Address:"));
        details.add(new JLabel(email != null && !email.isEmpty() ? email : "Not specified"));
        details.add(new JLabel("Availability Status:"));
        details.add(new JLabel(availability != null ? availability : "AVAILABLE"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);
        com.bloodlink.client.ui.components.ModernButton msgBtn = new com.bloodlink.client.ui.components.ModernButton("💬 Message Donor", com.bloodlink.client.ui.components.ModernButton.ButtonStyle.PRIMARY);
        msgBtn.addActionListener(e -> {
            dialog.dispose();
            showMessagingDialog(parent, donorId, donorName);
        });
        com.bloodlink.client.ui.components.ModernButton closeBtn = new com.bloodlink.client.ui.components.ModernButton("Close", com.bloodlink.client.ui.components.ModernButton.ButtonStyle.SECONDARY);
        closeBtn.addActionListener(e -> dialog.dispose());

        bottom.add(msgBtn);
        bottom.add(closeBtn);

        p.add(header, BorderLayout.NORTH);
        p.add(details, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(p);
        dialog.pack();
        dialog.setSize(500, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    public void logout() {
        User current = SessionManager.getInstance().getCurrentUser();
        if (current != null && !SessionManager.getInstance().isMockMode() && NetworkManager.getInstance().isConnected()) {
            NetworkManager.getInstance().sendRequest(
                    com.bloodlink.client.network.RequestBuilder.buildLogoutRequest(NetworkManager.getInstance().nextRequestId(), current.getUsername()),
                    "req_logout"
            );
        }
        SessionManager.getInstance().clearSession();
        showLogin();
    }

    private void setActiveFrame(JFrame frame) {
        this.activeFrame = frame;
    }

    public JFrame getActiveFrame() {
        return activeFrame;
    }

    private void closeActiveFrame() {
        if (activeFrame != null) {
            activeFrame.dispose();
            activeFrame = null;
        }
    }
}
