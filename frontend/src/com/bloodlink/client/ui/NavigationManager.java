package com.bloodlink.client.ui;

import com.bloodlink.client.models.User;
import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.screens.*;
import com.bloodlink.client.utils.SessionManager;

import javax.swing.*;

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
