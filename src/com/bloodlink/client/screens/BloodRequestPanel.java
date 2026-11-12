package com.bloodlink.client.screens;

import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.ui.components.ModernButton;
import com.bloodlink.client.ui.components.ModernTextField;
import com.bloodlink.client.utils.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Blood Request creation panel.
 * Allows recipients and administrators to publish emergency or scheduled blood requests.
 */
public class BloodRequestPanel extends JPanel {

    private final JDialog parentDialog;
    private final Runnable onSuccess;

    private JComboBox<String> bloodGroupCombo;
    private JSpinner unitsSpinner;
    private ModernTextField hospitalField;
    private ModernTextField cityField;
    private JComboBox<String> urgencyCombo;
    private JTextArea notesArea;
    private JLabel statusLabel;
    private ModernButton submitButton;

    public BloodRequestPanel(JDialog parentDialog, Runnable onSuccess) {
        this.parentDialog = parentDialog;
        this.onSuccess = onSuccess;
        initUI();
    }

    private void initUI() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(24, 28, 24, 28));

        // Title Header
        JPanel header = new JPanel(new GridLayout(2, 1, 0, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Create Blood Request");
        title.setFont(ThemeManager.FONT_HEADER);
        title.setForeground(ThemeManager.COLOR_PRIMARY);

        JLabel sub = new JLabel("Broadcast request to compatible blood donors in the network");
        sub.setFont(ThemeManager.FONT_TAGLINE);
        sub.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        header.add(title);
        header.add(sub);

        // Form Fields
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 0, 16, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(ThemeManager.FONT_CAPTION);
        statusLabel.setForeground(ThemeManager.COLOR_CRITICAL);
        form.add(statusLabel);
        form.add(Box.createVerticalStrut(10));

        // Blood Group & Units Row
        JPanel bgUnitsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        bgUnitsRow.setOpaque(false);

        JPanel bgPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        bgPanel.setOpaque(false);
        JLabel bgLabel = new JLabel("Required Blood Group *");
        bgLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        bloodGroupCombo = new JComboBox<>(new String[]{"O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"});
        bloodGroupCombo.setFont(ThemeManager.FONT_BODY);
        bgPanel.add(bgLabel);
        bgPanel.add(bloodGroupCombo);

        JPanel unitsPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        unitsPanel.setOpaque(false);
        JLabel unitsLabel = new JLabel("Units Needed (Pints) *");
        unitsLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        unitsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        unitsSpinner.setFont(ThemeManager.FONT_BODY);
        unitsPanel.add(unitsLabel);
        unitsPanel.add(unitsSpinner);

        bgUnitsRow.add(bgPanel);
        bgUnitsRow.add(unitsPanel);
        form.add(bgUnitsRow);
        form.add(Box.createVerticalStrut(12));

        // Hospital Field
        hospitalField = new ModernTextField("", "e.g. National Hospital Colombo");
        addFormField(form, "Hospital / Medical Center *", hospitalField);

        // City & Urgency Row
        JPanel cityUrgRow = new JPanel(new GridLayout(1, 2, 16, 0));
        cityUrgRow.setOpaque(false);

        JPanel cityPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        cityPanel.setOpaque(false);
        JLabel cityLabel = new JLabel("City / District *");
        cityLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        cityField = new ModernTextField("", "e.g. Colombo");
        cityPanel.add(cityLabel);
        cityPanel.add(cityField);

        JPanel urgPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        urgPanel.setOpaque(false);
        JLabel urgLabel = new JLabel("Urgency Level *");
        urgLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        urgencyCombo = new JComboBox<>(new String[]{"NORMAL", "URGENT", "CRITICAL"});
        urgencyCombo.setFont(ThemeManager.FONT_BODY);
        urgPanel.add(urgLabel);
        urgPanel.add(urgencyCombo);

        cityUrgRow.add(cityPanel);
        cityUrgRow.add(urgPanel);
        form.add(cityUrgRow);
        form.add(Box.createVerticalStrut(12));

        // Notes Area
        JPanel notesPanel = new JPanel(new BorderLayout(0, 4));
        notesPanel.setOpaque(false);
        JLabel notesLabel = new JLabel("Clinical Notes / Instructions");
        notesLabel.setFont(ThemeManager.FONT_BODY_BOLD);

        notesArea = new JTextArea(3, 20);
        notesArea.setFont(ThemeManager.FONT_BODY);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        JScrollPane notesScroll = new JScrollPane(notesArea);
        notesScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1));

        notesPanel.add(notesLabel, BorderLayout.NORTH);
        notesPanel.add(notesScroll, BorderLayout.CENTER);
        form.add(notesPanel);

        // Buttons Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        footer.setOpaque(false);

        ModernButton cancelButton = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        cancelButton.addActionListener(e -> parentDialog.dispose());

        submitButton = new ModernButton("Broadcast Request", ModernButton.ButtonStyle.PRIMARY);
        submitButton.addActionListener(e -> handleSubmit());

        footer.add(cancelButton);
        footer.add(submitButton);

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
        parent.add(Box.createVerticalStrut(12));
    }

    private void handleSubmit() {
        String bg = (String) bloodGroupCombo.getSelectedItem();
        int units = (Integer) unitsSpinner.getValue();
        String hospital = hospitalField.getText().trim();
        String city = cityField.getText().trim();
        String urgency = (String) urgencyCombo.getSelectedItem();
        String notes = notesArea.getText().trim();

        if (!ValidationUtils.isNotEmpty(hospital)) {
            showError("Hospital name is required.");
            hospitalField.requestFocus();
            return;
        }

        if (!ValidationUtils.isNotEmpty(city)) {
            showError("City is required.");
            cityField.requestFocus();
            return;
        }

        submitButton.setEnabled(false);
        submitButton.setText("Publishing...");
        statusLabel.setText("Submitting blood request...");
        statusLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        String canonicalBg = ValidationUtils.toCanonicalWireBloodGroup(bg);

        NetworkManager.getInstance().createBloodRequest(canonicalBg, units, hospital, city, urgency, notes)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("Broadcast Request");
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(
                                parentDialog,
                                "Blood Request created successfully!\nEligible donors will receive notification alerts.",
                                "Request Broadcast",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        parentDialog.dispose();
                    } else {
                        showError(response.getError());
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        submitButton.setEnabled(true);
                        submitButton.setText("Broadcast Request");
                        showError("Failed to submit request: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setForeground(ThemeManager.COLOR_CRITICAL);
    }
}
