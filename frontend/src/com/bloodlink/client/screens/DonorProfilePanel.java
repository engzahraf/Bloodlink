package com.bloodlink.client.screens;

import com.bloodlink.client.models.User;
import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.ui.components.ModernButton;
import com.bloodlink.client.ui.components.ModernTextField;
import com.bloodlink.client.ui.components.StatusBadge;
import com.bloodlink.client.utils.SessionManager;
import com.bloodlink.client.utils.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Donor Profile and Availability Management Panel.
 * Allows donors to update contact information and toggle their real-time donation availability.
 */
public class DonorProfilePanel extends JPanel {

    private final JDialog parentDialog;
    private final Runnable onUpdated;

    private ModernTextField fullNameField;
    private ModernTextField phoneField;
    private ModernTextField emailField;
    private ModernTextField cityField;
    private JCheckBox availabilityCheck;
    private StatusBadge bloodGroupBadge;
    private JLabel statusLabel;
    private ModernButton saveButton;

    public DonorProfilePanel(JDialog parentDialog, Runnable onUpdated) {
        this.parentDialog = parentDialog;
        this.onUpdated = onUpdated;
        initUI();
    }

    private void initUI() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(24, 28, 24, 28));

        User user = SessionManager.getInstance().getCurrentUser();

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Donor Profile & Settings");
        title.setFont(ThemeManager.FONT_HEADER);
        title.setForeground(ThemeManager.COLOR_PRIMARY);

        JLabel sub = new JLabel("Manage your contact information and donation availability status");
        sub.setFont(ThemeManager.FONT_TAGLINE);
        sub.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        header.add(title);
        header.add(sub);

        // Body Form
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 0, 16, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(ThemeManager.FONT_CAPTION);
        statusLabel.setForeground(ThemeManager.COLOR_CRITICAL);
        form.add(statusLabel);
        form.add(Box.createVerticalStrut(10));

        // Blood Group display
        JPanel bgRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bgRow.setOpaque(false);
        JLabel bgPrompt = new JLabel("Registered Blood Group:");
        bgPrompt.setFont(ThemeManager.FONT_BODY_BOLD);
        String displayBg = user != null ? ValidationUtils.toDisplayBloodGroup(user.getBloodGroup()) : "O+";
        bloodGroupBadge = new StatusBadge(displayBg, StatusBadge.StatusType.DANGER);
        bgRow.add(bgPrompt);
        bgRow.add(bloodGroupBadge);
        form.add(bgRow);
        form.add(Box.createVerticalStrut(14));

        fullNameField = new ModernTextField(user != null ? user.getFullName() : "", "Full Name");
        phoneField = new ModernTextField(user != null ? user.getPhone() : "", "Phone Number");
        emailField = new ModernTextField(user != null ? user.getEmail() : "", "Email");
        cityField = new ModernTextField(user != null ? user.getCity() : "", "City / District");

        addFormField(form, "Full Name *", fullNameField);
        addFormField(form, "Phone Number *", phoneField);
        addFormField(form, "Email Address *", emailField);
        addFormField(form, "City / Location *", cityField);

        // Availability Toggle
        availabilityCheck = new JCheckBox("I am currently available to donate blood");
        availabilityCheck.setFont(ThemeManager.FONT_BODY_BOLD);
        availabilityCheck.setForeground(ThemeManager.COLOR_SUCCESS);
        availabilityCheck.setSelected(true);
        availabilityCheck.setOpaque(false);
        form.add(availabilityCheck);

        // Footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footer.setOpaque(false);

        ModernButton cancelButton = new ModernButton("Close", ModernButton.ButtonStyle.SECONDARY);
        cancelButton.addActionListener(e -> parentDialog.dispose());

        saveButton = new ModernButton("Save Profile", ModernButton.ButtonStyle.PRIMARY);
        saveButton.addActionListener(e -> handleSave());

        footer.add(cancelButton);
        footer.add(saveButton);

        add(header, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private void addFormField(JPanel parent, String labelText, JComponent field) {
        JPanel row = new JPanel(new GridLayout(2, 1, 0, 4));
        row.setOpaque(false);
        JLabel label = new JLabel(labelText);
        label.setFont(ThemeManager.FONT_BODY_BOLD);
        label.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        row.add(label);
        row.add(field);
        parent.add(row);
        parent.add(Box.createVerticalStrut(10));
    }

    private void handleSave() {
        String fullName = fullNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        String city = cityField.getText().trim();
        boolean isAvailable = availabilityCheck.isSelected();

        if (!ValidationUtils.isNotEmpty(fullName)) {
            showError("Full Name is required.");
            fullNameField.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            showError("Please enter a valid phone number.");
            phoneField.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            emailField.requestFocus();
            return;
        }

        if (!ValidationUtils.isNotEmpty(city)) {
            showError("City is required.");
            cityField.requestFocus();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText("Updating...");

        User user = SessionManager.getInstance().getCurrentUser();
        String userId = user != null ? user.getUserId() : "usr_current";

        // Update local session
        if (user != null) {
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setEmail(email);
            user.setCity(city);
        }

        NetworkManager.getInstance().updateDonorAvailability(userId, isAvailable)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Save Profile");
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(
                                parentDialog,
                                "Profile and availability updated successfully!",
                                "Profile Updated",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        if (onUpdated != null) {
                            onUpdated.run();
                        }
                        parentDialog.dispose();
                    } else {
                        showError(response.getError());
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        saveButton.setEnabled(true);
                        saveButton.setText("Save Profile");
                        showError("Update failed: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setForeground(ThemeManager.COLOR_CRITICAL);
    }
}
