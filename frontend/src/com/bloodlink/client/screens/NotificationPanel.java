package com.bloodlink.client.screens;

import com.bloodlink.client.models.Message;
import com.bloodlink.client.models.Notification;
import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.network.ResponseHandler;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.ui.components.ModernButton;
import com.bloodlink.client.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Notifications and Urgent Alerts Center.
 * Receives real-time push events from the server and displays system alerts.
 */
public class NotificationPanel extends JPanel implements NetworkManager.PushEventListener {

    private final JDialog parentDialog;
    private ModernTable table;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private ModernButton refreshButton;

    private final List<Notification> notificationsList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm");

    public NotificationPanel(JDialog parentDialog) {
        this.parentDialog = parentDialog;
        initUI();
        NetworkManager.getInstance().addPushEventListener(this);
        loadNotifications();
    }

    private void initUI() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setOpaque(false);
        JLabel title = new JLabel("Notification & Alert Center");
        title.setFont(ThemeManager.FONT_HEADER);
        title.setForeground(ThemeManager.COLOR_PRIMARY);

        JLabel sub = new JLabel("Emergency blood match alerts and platform updates");
        sub.setFont(ThemeManager.FONT_TAGLINE);
        sub.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        headerPanel.add(title);
        headerPanel.add(sub);

        // Center Table
        String[] columns = {"Urgency", "Title", "Message", "Time", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new ModernTable(tableModel);
        JScrollPane scrollPane = ModernTable.createScrollPane(table);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(12, 0, 12, 0));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        statusLabel = new JLabel("Loading alerts...");
        statusLabel.setFont(ThemeManager.FONT_CAPTION);
        statusLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setOpaque(false);

        refreshButton = new ModernButton("Refresh", ModernButton.ButtonStyle.SECONDARY);
        refreshButton.setFont(ThemeManager.FONT_CAPTION);
        refreshButton.addActionListener(e -> loadNotifications());

        ModernButton markReadBtn = new ModernButton("Mark All as Read", ModernButton.ButtonStyle.SECONDARY);
        markReadBtn.setFont(ThemeManager.FONT_CAPTION);
        markReadBtn.addActionListener(e -> {
            for (Notification n : notificationsList) {
                n.setRead(true);
            }
            refreshTable();
        });

        ModernButton closeBtn = new ModernButton("Close", ModernButton.ButtonStyle.PRIMARY);
        closeBtn.setFont(ThemeManager.FONT_CAPTION);
        closeBtn.addActionListener(e -> {
            NetworkManager.getInstance().removePushEventListener(this);
            if (parentDialog != null) {
                parentDialog.dispose();
            }
        });

        actionButtons.add(refreshButton);
        actionButtons.add(markReadBtn);
        actionButtons.add(closeBtn);

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(actionButtons, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void loadNotifications() {
        refreshButton.setEnabled(false);
        statusLabel.setText("Fetching notifications from server...");

        NetworkManager.getInstance().getNotifications(30)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    refreshButton.setEnabled(true);
                    notificationsList.clear();

                    if (response.isSuccess()) {
                        notificationsList.addAll(ResponseHandler.parseNotifications(response));
                        refreshTable();
                        statusLabel.setText(notificationsList.size() + " total alerts");
                    } else {
                        statusLabel.setText("No notifications found: " + response.getError());
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        refreshButton.setEnabled(true);
                        statusLabel.setText("Failed to fetch alerts: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Notification n : notificationsList) {
            tableModel.addRow(new Object[]{
                    n.getUrgency(),
                    n.getTitle(),
                    n.getMessage(),
                    dateFormat.format(new Date(n.getTimestamp())),
                    n.isRead() ? "READ" : "UNREAD"
            });
        }
    }

    // Push Event Listener Implementations
    @Override
    public void onUrgentAlertReceived(String bloodGroup, String hospital, String city, String urgency, String title) {
        Notification alert = new Notification(
                "alert_" + System.currentTimeMillis(),
                title != null ? title : ("URGENT: Blood Group " + bloodGroup + " Needed"),
                hospital + " in " + city + " requires immediate blood donation.",
                urgency != null ? urgency : "URGENT",
                false,
                System.currentTimeMillis()
        );
        notificationsList.add(0, alert);
        refreshTable();
        statusLabel.setText("⚠️ New urgent alert received!");
    }

    @Override
    public void onIncomingMessageReceived(Message message) {
        // Handled in messaging panel
    }

    @Override
    public void onConnectionStateChanged(boolean connected, String message) {
        statusLabel.setText(connected ? "Server connected" : "Server disconnected");
    }
}
