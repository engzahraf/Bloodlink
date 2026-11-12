package com.bloodlink.client.screens;

import com.bloodlink.client.models.User;
import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.network.ResponseHandler;
import com.bloodlink.client.network.ResponseHandler.ParsedResponse;
import com.bloodlink.client.ui.NavigationManager;
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
 * Authentication login screen for BloodLink.
 * Supports credentials entry, password visibility toggle, server connection status,
 * and offline mock mode toggling.
 */
public class LoginFrame extends JFrame {

    private final NavigationManager navigationManager;

    private ModernTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheck;
    private JCheckBox mockModeCheck;
    private ModernButton loginButton;
    private JLabel statusLabel;
    private StatusBadge connectionBadge;

    public LoginFrame(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        initUI();
    }

    private void initUI() {
        setTitle("BloodLink - Connecting Lives Through Blood Donation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, 600);
        setMinimumSize(new Dimension(420, 560));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);
        setLayout(new BorderLayout());

        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(32, 24, 16, 24));

        JLabel iconLabel = new JLabel("🩸");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("BloodLink");
        titleLabel.setFont(ThemeManager.FONT_HEADER);
        titleLabel.setForeground(ThemeManager.COLOR_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Connecting Lives Through Blood Donation");
        subtitleLabel.setFont(ThemeManager.FONT_TAGLINE);
        subtitleLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);

        // Center card form
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(new EmptyBorder(0, 24, 16, 24));

        JPanel cardPanel = new JPanel();
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(ThemeManager.createCardBorder());
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        JLabel welcomeLabel = new JLabel("Welcome Back");
        welcomeLabel.setFont(ThemeManager.FONT_TITLE);
        welcomeLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        JLabel promptLabel = new JLabel("Sign in to your BloodLink account");
        promptLabel.setFont(ThemeManager.FONT_CAPTION);
        promptLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(ThemeManager.FONT_CAPTION);
        statusLabel.setForeground(ThemeManager.COLOR_CRITICAL);

        // Inputs
        JLabel userLabel = new JLabel("Username or Email");
        userLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        userLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        usernameField = new ModernTextField("", "Enter your username or email", 20);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        passLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        passwordField = new JPasswordField(20);
        passwordField.setFont(ThemeManager.FONT_BODY);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        showPasswordCheck = new JCheckBox("Show password");
        showPasswordCheck.setFont(ThemeManager.FONT_CAPTION);
        showPasswordCheck.setOpaque(false);
        showPasswordCheck.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);
        showPasswordCheck.addActionListener(e -> {
            if (showPasswordCheck.isSelected()) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar('•');
            }
        });

        mockModeCheck = new JCheckBox("Use Offline Mock Mode");
        mockModeCheck.setFont(ThemeManager.FONT_CAPTION);
        mockModeCheck.setOpaque(false);
        mockModeCheck.setForeground(ThemeManager.COLOR_WARNING);
        mockModeCheck.setSelected(SessionManager.getInstance().isMockMode());
        mockModeCheck.addActionListener(e -> {
            boolean isMock = mockModeCheck.isSelected();
            SessionManager.getInstance().setMockMode(isMock);
            updateConnectionStatus();
        });

        loginButton = new ModernButton("Sign In", ModernButton.ButtonStyle.PRIMARY);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Short.MAX_VALUE, 42));
        loginButton.addActionListener(e -> handleLogin());

        // Key binding: Enter key submits login
        getRootPane().setDefaultButton(loginButton);

        // Assembly
        cardPanel.add(welcomeLabel);
        cardPanel.add(Box.createVerticalStrut(2));
        cardPanel.add(promptLabel);
        cardPanel.add(Box.createVerticalStrut(6));
        cardPanel.add(statusLabel);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(userLabel);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(usernameField);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(passLabel);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(passwordField);
        cardPanel.add(Box.createVerticalStrut(6));

        JPanel checksRow = new JPanel(new BorderLayout());
        checksRow.setOpaque(false);
        checksRow.add(showPasswordCheck, BorderLayout.WEST);
        checksRow.add(mockModeCheck, BorderLayout.EAST);
        cardPanel.add(checksRow);

        cardPanel.add(Box.createVerticalStrut(16));
        cardPanel.add(loginButton);

        cardWrapper.add(cardPanel);

        // Bottom panel: register link and connection status
        JPanel footerPanel = new JPanel();
        footerPanel.setOpaque(false);
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.Y_AXIS));
        footerPanel.setBorder(new EmptyBorder(8, 24, 20, 24));

        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        registerRow.setOpaque(false);
        JLabel noAccountLabel = new JLabel("Don't have an account?");
        noAccountLabel.setFont(ThemeManager.FONT_BODY);
        noAccountLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        JButton registerLink = new JButton("Register Now");
        registerLink.setFont(ThemeManager.FONT_BODY_BOLD);
        registerLink.setForeground(ThemeManager.COLOR_PRIMARY);
        registerLink.setBorderPainted(false);
        registerLink.setContentAreaFilled(false);
        registerLink.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerLink.addActionListener(e -> navigationManager.showRegister());

        registerRow.add(noAccountLabel);
        registerRow.add(registerLink);

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        statusRow.setOpaque(false);
        connectionBadge = new StatusBadge("CHECKING", StatusBadge.StatusType.NEUTRAL);
        statusRow.add(new JLabel("Server: " + SessionManager.getInstance().getServerHost() + ":" + SessionManager.getInstance().getServerPort()));
        statusRow.add(connectionBadge);

        footerPanel.add(registerRow);
        footerPanel.add(Box.createVerticalStrut(8));
        footerPanel.add(statusRow);

        add(headerPanel, BorderLayout.NORTH);
        add(cardWrapper, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);

        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        if (SessionManager.getInstance().isMockMode()) {
            connectionBadge.setBadge("MOCK MODE", StatusBadge.StatusType.WARNING);
        } else if (NetworkManager.getInstance().isConnected()) {
            connectionBadge.setBadge("ONLINE", StatusBadge.StatusType.SUCCESS);
        } else {
            connectionBadge.setBadge("OFFLINE", StatusBadge.StatusType.DANGER);
        }
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        char[] passChars = passwordField.getPassword();
        String password = new String(passChars);

        if (!ValidationUtils.isNotEmpty(username)) {
            showError("Please enter your username or email.");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Please enter your password.");
            passwordField.requestFocus();
            return;
        }

        // Show loading state
        setLoading(true);
        statusLabel.setText("Authenticating with server...");
        statusLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        boolean isMock = SessionManager.getInstance().isMockMode();

        // If not in mock mode and not connected, attempt auto-connect first
        if (!isMock && !NetworkManager.getInstance().isConnected()) {
            try {
                NetworkManager.getInstance().connect(
                        SessionManager.getInstance().getServerHost(),
                        SessionManager.getInstance().getServerPort()
                );
                updateConnectionStatus();
            } catch (Exception e) {
                setLoading(false);
                showError("Unable to connect to server (" + e.getMessage() + "). Check server or enable Mock Mode.");
                updateConnectionStatus();
                return;
            }
        }

        NetworkManager.getInstance().login(username, password)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    setLoading(false);
                    if (response.isSuccess()) {
                        statusLabel.setText("Login successful! Redirecting...");
                        statusLabel.setForeground(ThemeManager.COLOR_SUCCESS);

                        User user = ResponseHandler.parseUser(response);
                        if (user != null) {
                            navigationManager.navigateToDashboard(user);
                        } else {
                            showError("Invalid user profile received from server.");
                        }
                    } else {
                        showError(response.getError());
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        setLoading(false);
                        showError("Login failed: " + throwable.getMessage());
                        updateConnectionStatus();
                    });
                    return null;
                });
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Authenticating..." : "Sign In");
        usernameField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(ThemeManager.COLOR_CRITICAL);
    }
}
