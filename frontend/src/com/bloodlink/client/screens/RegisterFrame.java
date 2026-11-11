package com.bloodlink.client.screens;

import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.ui.NavigationManager;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.ui.components.ModernButton;
import com.bloodlink.client.ui.components.ModernTextField;
import com.bloodlink.client.utils.SessionManager;
import com.bloodlink.client.utils.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Registration screen for new donors and blood recipients.
 * Enforces client-side validation for emails, phone numbers, password confirmation,
 * and donor-specific blood group requirements before wire transmission.
 */
public class RegisterFrame extends JFrame {

    private final NavigationManager navigationManager;

    private ModernTextField fullNameField;
    private ModernTextField usernameField;
    private ModernTextField emailField;
    private ModernTextField phoneField;
    private ModernTextField cityField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JComboBox<String> roleCombo;
    private JComboBox<String> bloodGroupCombo;
    private JPanel bloodGroupRow;
    private JLabel statusLabel;
    private ModernButton registerButton;

    public RegisterFrame(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        initUI();
    }

    private void initUI() {
        setTitle("BloodLink - Register New Account");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 720);
        setMinimumSize(new Dimension(480, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);
        setLayout(new BorderLayout());

        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(24, 24, 12, 24));

        JLabel titleLabel = new JLabel("Create BloodLink Account");
        titleLabel.setFont(ThemeManager.FONT_HEADER);
        titleLabel.setForeground(ThemeManager.COLOR_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Join our lifesaving network of donors and recipients");
        subtitleLabel.setFont(ThemeManager.FONT_TAGLINE);
        subtitleLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);

        // Center card form in scroll pane
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(new EmptyBorder(0, 24, 12, 24));

        JPanel formCard = new JPanel();
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(ThemeManager.createCardBorder());
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(ThemeManager.FONT_CAPTION);
        statusLabel.setForeground(ThemeManager.COLOR_CRITICAL);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Fields
        fullNameField = new ModernTextField("", "e.g. John Doe");
        usernameField = new ModernTextField("", "e.g. jdoe");
        emailField = new ModernTextField("", "e.g. jdoe@example.com");
        phoneField = new ModernTextField("", "e.g. +94771234567");
        cityField = new ModernTextField("", "e.g. Colombo");

        passwordField = new JPasswordField(20);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        roleCombo = new JComboBox<>(new String[]{"DONOR", "RECIPIENT"});
        roleCombo.setFont(ThemeManager.FONT_BODY);
        roleCombo.setBackground(Color.WHITE);

        bloodGroupCombo = new JComboBox<>(new String[]{"O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"});
        bloodGroupCombo.setFont(ThemeManager.FONT_BODY);
        bloodGroupCombo.setBackground(Color.WHITE);

        bloodGroupRow = new JPanel(new GridLayout(2, 1, 0, 4));
        bloodGroupRow.setOpaque(false);
        JLabel bgLabel = new JLabel("Blood Group *");
        bgLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        bloodGroupRow.add(bgLabel);
        bloodGroupRow.add(bloodGroupCombo);

        roleCombo.addActionListener(e -> {
            boolean isDonor = "DONOR".equals(roleCombo.getSelectedItem());
            bloodGroupRow.setVisible(isDonor);
            formCard.revalidate();
            formCard.repaint();
        });

        // Add form rows
        formCard.add(statusLabel);
        formCard.add(Box.createVerticalStrut(8));
        addFormField(formCard, "Full Name *", fullNameField);
        addFormField(formCard, "Username *", usernameField);
        addFormField(formCard, "Email Address *", emailField);
        addFormField(formCard, "Phone Number *", phoneField);
        addFormField(formCard, "City / Location *", cityField);

        JPanel roleRow = new JPanel(new GridLayout(2, 1, 0, 4));
        roleRow.setOpaque(false);
        JLabel rLabel = new JLabel("Register As *");
        rLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        roleRow.add(rLabel);
        roleRow.add(roleCombo);
        formCard.add(roleRow);
        formCard.add(Box.createVerticalStrut(10));

        formCard.add(bloodGroupRow);
        formCard.add(Box.createVerticalStrut(10));

        addFormField(formCard, "Password (min 6 characters) *", passwordField);
        addFormField(formCard, "Confirm Password *", confirmPasswordField);

        registerButton = new ModernButton("Create Account", ModernButton.ButtonStyle.PRIMARY);
        registerButton.setMaximumSize(new Dimension(Short.MAX_VALUE, 42));
        registerButton.addActionListener(e -> handleRegister());
        formCard.add(Box.createVerticalStrut(12));
        formCard.add(registerButton);

        cardWrapper.add(formCard);

        JScrollPane scrollPane = new JScrollPane(cardWrapper);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(ThemeManager.COLOR_BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Footer: Back to login
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 12));
        footerPanel.setOpaque(false);
        JLabel hasAccount = new JLabel("Already have an account?");
        hasAccount.setFont(ThemeManager.FONT_BODY);
        hasAccount.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        JButton backToLogin = new JButton("Sign In Here");
        backToLogin.setFont(ThemeManager.FONT_BODY_BOLD);
        backToLogin.setForeground(ThemeManager.COLOR_PRIMARY);
        backToLogin.setBorderPainted(false);
        backToLogin.setContentAreaFilled(false);
        backToLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backToLogin.addActionListener(e -> navigationManager.showLogin());

        footerPanel.add(hasAccount);
        footerPanel.add(backToLogin);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
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

    private void handleRegister() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String city = cityField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String role = (String) roleCombo.getSelectedItem();
        String bloodGroup = (String) bloodGroupCombo.getSelectedItem();

        // Validation
        if (!ValidationUtils.isNotEmpty(fullName)) {
            showError("Full Name is required.");
            fullNameField.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            showError("Username must be between 3 and 30 alphanumeric characters.");
            usernameField.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            emailField.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            showError("Please enter a valid phone number (7-15 digits, optional +).");
            phoneField.requestFocus();
            return;
        }

        if (!ValidationUtils.isNotEmpty(city)) {
            showError("City/Location is required.");
            cityField.requestFocus();
            return;
        }

        if ("DONOR".equals(role) && !ValidationUtils.isValidBloodGroup(bloodGroup)) {
            showError("Please select a valid blood group for donor registration.");
            return;
        }

        if (password.length() < 6) {
            showError("Password must be at least 6 characters long.");
            passwordField.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            confirmPasswordField.requestFocus();
            return;
        }

        setLoading(true);
        statusLabel.setText("Submitting registration...");
        statusLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        // Normalize blood group to wire token format (e.g. O+ -> O_POS)
        String wireBloodGroup = "DONOR".equals(role) ? ValidationUtils.toCanonicalWireBloodGroup(bloodGroup) : "";

        // If not connected and not mock, connect first
        if (!SessionManager.getInstance().isMockMode() && !NetworkManager.getInstance().isConnected()) {
            try {
                NetworkManager.getInstance().connect(
                        SessionManager.getInstance().getServerHost(),
                        SessionManager.getInstance().getServerPort()
                );
            } catch (Exception e) {
                setLoading(false);
                showError("Unable to connect to server (" + e.getMessage() + "). Check server or use Mock Mode.");
                return;
            }
        }

        NetworkManager.getInstance().register(username, password, role, fullName, wireBloodGroup, city, phone, email)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    setLoading(false);
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(
                                RegisterFrame.this,
                                "Account registered successfully!\nPlease sign in with your credentials.",
                                "Registration Success",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        navigationManager.showLogin();
                    } else {
                        showError(response.getError());
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        setLoading(false);
                        showError("Registration failed: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void setLoading(boolean loading) {
        registerButton.setEnabled(!loading);
        registerButton.setText(loading ? "Registering..." : "Create Account");
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(ThemeManager.COLOR_CRITICAL);
    }
}
