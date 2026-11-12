package com.bloodlink.client.screens;

import com.bloodlink.client.models.BloodRequest;
import com.bloodlink.client.models.Message;
import com.bloodlink.client.models.User;
import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.network.ResponseHandler;
import com.bloodlink.client.ui.NavigationManager;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.ui.components.*;
import com.bloodlink.client.utils.SessionManager;
import com.bloodlink.client.utils.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Donor Dashboard for BloodLink.
 * Displays incoming blood requests, matching requirements based on ABO/Rh rules,
 * and enables donors to Accept, Reject, and direct message recipients.
 */
public class DonorDashboard extends JFrame implements NetworkManager.PushEventListener {

    private final NavigationManager navigationManager;

    private HeaderPanel headerPanel;
    private StatCard bloodGroupCard;
    private StatCard matchingRequestsCard;
    private StatCard availabilityCard;

    private JLabel bannerAlertLabel;
    private ModernTable requestsTable;
    private DefaultTableModel requestsModel;
    private ModernButton acceptButton;
    private ModernButton rejectButton;
    private ModernButton contactButton;

    private final List<BloodRequest> matchingRequests = new ArrayList<>();
    private final Map<String, String> myResponsesMap = new HashMap<>();

    public DonorDashboard(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        initUI();
        NetworkManager.getInstance().addPushEventListener(this);
        refreshDashboard();
    }

    private void initUI() {
        setTitle("BloodLink - Donor Dashboard & Donation Match Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 750);
        setMinimumSize(new Dimension(940, 660));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);
        setLayout(new BorderLayout());

        // Header Panel
        headerPanel = new HeaderPanel(navigationManager);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content
        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(new EmptyBorder(16, 24, 20, 24));

        // Real-Time Urgent Alert Banner
        bannerAlertLabel = new JLabel("🚨 URGENT BLOOD ALERT");
        bannerAlertLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        bannerAlertLabel.setForeground(Color.WHITE);
        bannerAlertLabel.setBackground(ThemeManager.COLOR_CRITICAL);
        bannerAlertLabel.setOpaque(true);
        bannerAlertLabel.setBorder(new EmptyBorder(10, 16, 10, 16));
        bannerAlertLabel.setVisible(false);
        bannerAlertLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        bannerAlertLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(bannerAlertLabel);
        mainContent.add(Box.createVerticalStrut(14));

        // StatCards Row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        User user = SessionManager.getInstance().getCurrentUser();
        String displayBg = user != null ? ValidationUtils.toDisplayBloodGroup(user.getBloodGroup()) : "O+";

        bloodGroupCard = new StatCard("My Blood Group", displayBg, "Donor compatibility verified", ThemeManager.COLOR_PRIMARY);
        matchingRequestsCard = new StatCard("Compatible Blood Requests", "0", "Awaiting donor response", ThemeManager.COLOR_CRITICAL);
        availabilityCard = new StatCard("Donor Status", "AVAILABLE", "Ready for emergency requests", ThemeManager.COLOR_SUCCESS);

        statsRow.add(bloodGroupCard);
        statsRow.add(matchingRequestsCard);
        statsRow.add(availabilityCard);
        mainContent.add(statsRow);
        mainContent.add(Box.createVerticalStrut(18));

        // Action Toolbar
        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionToolbar.setOpaque(false);
        actionToolbar.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));

        ModernButton editProfileBtn = new ModernButton("👤 Profile & Availability", ModernButton.ButtonStyle.PRIMARY);
        editProfileBtn.setFont(ThemeManager.FONT_CAPTION);
        editProfileBtn.addActionListener(e -> navigationManager.showDonorProfileDialog(this, this::refreshDashboard));

        ModernButton refreshBtn = new ModernButton("🔄 Refresh Matching Requests", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setFont(ThemeManager.FONT_CAPTION);
        refreshBtn.addActionListener(e -> refreshDashboard());

        ModernButton notifBtn = new ModernButton("🔔 View Alerts & Notifications", ModernButton.ButtonStyle.SECONDARY);
        notifBtn.setFont(ThemeManager.FONT_CAPTION);
        notifBtn.addActionListener(e -> navigationManager.showNotifications());

        ModernButton searchDonorsBtn = new ModernButton("🔍 Browse Network", ModernButton.ButtonStyle.SECONDARY);
        searchDonorsBtn.setFont(ThemeManager.FONT_CAPTION);
        searchDonorsBtn.addActionListener(e -> navigationManager.showDonorSearchDialog(this));

        actionToolbar.add(editProfileBtn);
        actionToolbar.add(refreshBtn);
        actionToolbar.add(notifBtn);
        actionToolbar.add(searchDonorsBtn);
        mainContent.add(actionToolbar);
        mainContent.add(Box.createVerticalStrut(16));

        // Matching Requests Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(ThemeManager.createCardBorder());

        JLabel tableTitle = new JLabel("Compatible Blood Requests (Select to Accept, Reject, or Message)");
        tableTitle.setFont(ThemeManager.FONT_TITLE);
        tableTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        String[] cols = {"Request ID", "Required Blood", "Units", "Hospital", "City", "Urgency", "Request Status", "My Response"};
        requestsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestsTable = new ModernTable(requestsModel);
        requestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestsTable.getSelectionModel().addListSelectionListener(e -> updateButtonStates());

        JScrollPane scrollPane = ModernTable.createScrollPane(requestsTable);

        // Table bottom action row
        JPanel tableBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tableBottom.setOpaque(false);

        acceptButton = new ModernButton("✓ Accept Request", ModernButton.ButtonStyle.PRIMARY);
        acceptButton.setEnabled(false);
        acceptButton.addActionListener(e -> handleAcceptRequest());

        rejectButton = new ModernButton("✗ Reject Request", ModernButton.ButtonStyle.SECONDARY);
        rejectButton.setEnabled(false);
        rejectButton.addActionListener(e -> handleRejectRequest());

        contactButton = new ModernButton("💬 Message Recipient / Hospital", ModernButton.ButtonStyle.OUTLINE);
        contactButton.setEnabled(false);
        contactButton.addActionListener(e -> {
            int row = requestsTable.getSelectedRow();
            if (row >= 0 && row < matchingRequests.size()) {
                BloodRequest br = matchingRequests.get(row);
                navigationManager.showMessagingDialog(this, br.getRecipientId(), br.getHospital());
            }
        });

        tableBottom.add(acceptButton);
        tableBottom.add(rejectButton);
        tableBottom.add(contactButton);

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(tableBottom, BorderLayout.SOUTH);

        mainContent.add(tableCard);

        add(mainContent, BorderLayout.CENTER);
    }

    private void updateButtonStates() {
        int row = requestsTable.getSelectedRow();
        if (row < 0 || row >= matchingRequests.size()) {
            acceptButton.setEnabled(false);
            acceptButton.setText("✓ Accept Request");
            rejectButton.setEnabled(false);
            rejectButton.setText("✗ Reject Request");
            contactButton.setEnabled(false);
            return;
        }

        BloodRequest br = matchingRequests.get(row);
        contactButton.setEnabled(true);

        String currentResp = myResponsesMap.get(br.getRequestId());
        if ("ACCEPTED".equalsIgnoreCase(currentResp)) {
            acceptButton.setEnabled(false);
            acceptButton.setText("Already Accepted");
            rejectButton.setEnabled(true);
            rejectButton.setText("Change to Reject");
        } else if ("REJECTED".equalsIgnoreCase(currentResp)) {
            rejectButton.setEnabled(false);
            rejectButton.setText("Rejected");
            acceptButton.setEnabled(true);
            acceptButton.setText("✓ Accept Request");
        } else {
            acceptButton.setEnabled(true);
            acceptButton.setText("✓ Accept Request");
            rejectButton.setEnabled(true);
            rejectButton.setText("✗ Reject Request");
        }
    }

    public void refreshDashboard() {
        headerPanel.refreshState();

        User user = SessionManager.getInstance().getCurrentUser();
        String myBg = user != null ? ValidationUtils.toCanonicalWireBloodGroup(user.getBloodGroup()) : "";
        String displayBg = user != null ? ValidationUtils.toDisplayBloodGroup(user.getBloodGroup()) : "O+";
        bloodGroupCard.setValue(displayBg);

        // Fetch this donor's past responses map
        NetworkManager.getInstance().getDonorResponsesMap()
                .thenAccept(respMapResponse -> {
                    if (respMapResponse.isSuccess()) {
                        myResponsesMap.clear();
                        myResponsesMap.putAll(respMapResponse.getPayload());
                    }

                    // Query blood requests
                    NetworkManager.getInstance().getBloodRequests("", "", "")
                            .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                                requestsModel.setRowCount(0);
                                matchingRequests.clear();

                                if (response.isSuccess()) {
                                    List<BloodRequest> allRequests = ResponseHandler.parseBloodRequests(response);
                                    for (BloodRequest br : allRequests) {
                                        String status = br.getStatus();
                                        if ("COMPLETED".equalsIgnoreCase(status) || "FULFILLED".equalsIgnoreCase(status)
                                                || "CANCELLED".equalsIgnoreCase(status)) {
                                            continue;
                                        }

                                        // Check compatibility: donor can donate to recipient
                                        if (myBg != null && !myBg.isEmpty() && !ValidationUtils.isCompatible(myBg, br.getBloodGroup())) {
                                            continue;
                                        }

                                        matchingRequests.add(br);
                                        String myResp = myResponsesMap.getOrDefault(br.getRequestId(), "NONE");
                                        String myRespDisplay;
                                        if ("ACCEPTED".equalsIgnoreCase(myResp)) {
                                            myRespDisplay = "ACCEPTED";
                                        } else if ("REJECTED".equalsIgnoreCase(myResp)) {
                                            myRespDisplay = "REJECTED";
                                        } else {
                                            myRespDisplay = "PENDING";
                                        }

                                        requestsModel.addRow(new Object[]{
                                                br.getRequestId(),
                                                ValidationUtils.toDisplayBloodGroup(br.getBloodGroup()),
                                                br.getUnitsRequired() + " pints",
                                                br.getHospital(),
                                                br.getCity(),
                                                br.getUrgency(),
                                                br.getStatus(),
                                                myRespDisplay
                                        });
                                    }
                                    matchingRequestsCard.setValue(Integer.toString(matchingRequests.size()));
                                    updateButtonStates();
                                }
                            }))
                            .exceptionally(throwable -> null);
                })
                .exceptionally(throwable -> {
                    // Fallback to fetch requests directly if responses map call fails
                    NetworkManager.getInstance().getBloodRequests("", "", "")
                            .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                                requestsModel.setRowCount(0);
                                matchingRequests.clear();
                                if (response.isSuccess()) {
                                    List<BloodRequest> allRequests = ResponseHandler.parseBloodRequests(response);
                                    for (BloodRequest br : allRequests) {
                                        String status = br.getStatus();
                                        if ("COMPLETED".equalsIgnoreCase(status) || "FULFILLED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                                            continue;
                                        }
                                        if (myBg != null && !myBg.isEmpty() && !ValidationUtils.isCompatible(myBg, br.getBloodGroup())) {
                                            continue;
                                        }
                                        matchingRequests.add(br);
                                        requestsModel.addRow(new Object[]{
                                                br.getRequestId(),
                                                ValidationUtils.toDisplayBloodGroup(br.getBloodGroup()),
                                                br.getUnitsRequired() + " pints",
                                                br.getHospital(),
                                                br.getCity(),
                                                br.getUrgency(),
                                                br.getStatus(),
                                                "PENDING"
                                        });
                                    }
                                    matchingRequestsCard.setValue(Integer.toString(matchingRequests.size()));
                                    updateButtonStates();
                                }
                            }));
                    return null;
                });
    }

    private void handleAcceptRequest() {
        int row = requestsTable.getSelectedRow();
        if (row < 0 || row >= matchingRequests.size()) {
            return;
        }

        BloodRequest br = matchingRequests.get(row);
        acceptButton.setEnabled(false);
        acceptButton.setText("Accepting...");

        NetworkManager.getInstance().acceptBloodRequest(br.getRequestId(), "I can donate blood for this request")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        myResponsesMap.put(br.getRequestId(), "ACCEPTED");
                        requestsModel.setValueAt("ACCEPTED", row, 7);
                        requestsModel.setValueAt("ACCEPTED", row, 6);
                        updateButtonStates();

                        JOptionPane.showMessageDialog(
                                this,
                                "Thank you! You have accepted Blood Request #" + br.getRequestId() + " (" +
                                        ValidationUtils.toDisplayBloodGroup(br.getBloodGroup()) + " at " + br.getHospital() + ").\n" +
                                        "The recipient has been notified immediately.",
                                "Request Accepted",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        updateButtonStates();
                        JOptionPane.showMessageDialog(this, "Could not accept request: " + response.getError(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        updateButtonStates();
                        JOptionPane.showMessageDialog(this, "Network error: " + throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
    }

    private void handleRejectRequest() {
        int row = requestsTable.getSelectedRow();
        if (row < 0 || row >= matchingRequests.size()) {
            return;
        }

        BloodRequest br = matchingRequests.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to decline Blood Request #" + br.getRequestId() + "?",
                "Confirm Decline",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        rejectButton.setEnabled(false);
        rejectButton.setText("Rejecting...");

        NetworkManager.getInstance().rejectBloodRequest(br.getRequestId(), "Unable to donate at this time")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        myResponsesMap.put(br.getRequestId(), "REJECTED");
                        requestsModel.setValueAt("REJECTED", row, 7);
                        updateButtonStates();

                        JOptionPane.showMessageDialog(
                                this,
                                "You have declined Blood Request #" + br.getRequestId() + ".\n" +
                                        "The status has been recorded and updated.",
                                "Request Declined",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        updateButtonStates();
                        JOptionPane.showMessageDialog(this, "Could not decline request: " + response.getError(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        updateButtonStates();
                        JOptionPane.showMessageDialog(this, "Network error: " + throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
    }

    // Push Event Listener Implementations
    @Override
    public void onUrgentAlertReceived(String bloodGroup, String hospital, String city, String urgency, String title) {
        SwingUtilities.invokeLater(() -> {
            bannerAlertLabel.setText("🚨 " + (title != null ? title : "URGENT BLOOD ALERT") +
                    (bloodGroup != null && !bloodGroup.isEmpty() ? " (" + ValidationUtils.toDisplayBloodGroup(bloodGroup) + ")" : "") +
                    (hospital != null && !hospital.isEmpty() ? " at " + hospital : "") +
                    (city != null && !city.isEmpty() ? " in " + city : ""));
            bannerAlertLabel.setVisible(true);
            refreshDashboard();
        });
    }

    @Override
    public void onIncomingMessageReceived(Message message) {
    }

    @Override
    public void onConnectionStateChanged(boolean connected, String message) {
        SwingUtilities.invokeLater(() -> {
            headerPanel.setConnectionStatus(connected);
            availabilityCard.setValue(connected ? "AVAILABLE" : "OFFLINE");
        });
    }
}
