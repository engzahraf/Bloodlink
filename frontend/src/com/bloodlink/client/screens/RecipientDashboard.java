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
import java.util.List;

/**
 * Recipient Dashboard for BloodLink.
 * Enables patients and hospital representatives to submit blood requests,
 * search available donors, and monitor fulfillment status.
 */
public class RecipientDashboard extends JFrame implements NetworkManager.PushEventListener {

    private final NavigationManager navigationManager;

    private HeaderPanel headerPanel;
    private StatCard activeRequestsCard;
    private StatCard matchingDonorsCard;
    private StatCard statusCard;

    private ModernTable requestTable;
    private DefaultTableModel requestModel;
    private ModernButton searchMatchingButton;
    private ModernButton markFulfilledButton;

    private final List<BloodRequest> myRequests = new ArrayList<>();

    public RecipientDashboard(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        initUI();
        NetworkManager.getInstance().addPushEventListener(this);
        refreshDashboard();
    }

    private void initUI() {
        setTitle("BloodLink - Recipient Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 720);
        setMinimumSize(new Dimension(920, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);
        setLayout(new BorderLayout());

        // Top Header
        headerPanel = new HeaderPanel(navigationManager);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content
        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(new EmptyBorder(18, 24, 20, 24));

        // StatCards Row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        activeRequestsCard = new StatCard("Active Requests", "0", "Awaiting fulfillment", ThemeManager.COLOR_CRITICAL);
        matchingDonorsCard = new StatCard("Available Donors", "0", "Eligible in network", ThemeManager.COLOR_SUCCESS);
        statusCard = new StatCard("Network Status", "ONLINE", "TCP socket active", ThemeManager.COLOR_PRIMARY);

        statsRow.add(activeRequestsCard);
        statsRow.add(matchingDonorsCard);
        statsRow.add(statusCard);
        mainContent.add(statsRow);
        mainContent.add(Box.createVerticalStrut(18));

        // Action Toolbar
        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionToolbar.setOpaque(false);
        actionToolbar.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));

        ModernButton createReqBtn = new ModernButton("+ Request Blood Now", ModernButton.ButtonStyle.PRIMARY);
        createReqBtn.setFont(ThemeManager.FONT_CAPTION);
        createReqBtn.addActionListener(e -> navigationManager.showCreateRequestDialog(this, this::refreshDashboard));

        ModernButton searchDonorsBtn = new ModernButton("🔍 Search Compatible Donors", ModernButton.ButtonStyle.SECONDARY);
        searchDonorsBtn.setFont(ThemeManager.FONT_CAPTION);
        searchDonorsBtn.addActionListener(e -> navigationManager.showDonorSearchDialog(this));

        ModernButton refreshBtn = new ModernButton("🔄 Refresh Status", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setFont(ThemeManager.FONT_CAPTION);
        refreshBtn.addActionListener(e -> refreshDashboard());

        actionToolbar.add(createReqBtn);
        actionToolbar.add(searchDonorsBtn);
        actionToolbar.add(refreshBtn);
        mainContent.add(actionToolbar);
        mainContent.add(Box.createVerticalStrut(16));

        // My Requests Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(ThemeManager.createCardBorder());

        JLabel tableTitle = new JLabel("My Submitted Blood Requests");
        tableTitle.setFont(ThemeManager.FONT_TITLE);
        tableTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        String[] cols = {"Request ID", "Required Blood", "Units", "Hospital", "City", "Urgency", "Status"};
        requestModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestTable = new ModernTable(requestModel);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = requestTable.getSelectedRow() >= 0;
            searchMatchingButton.setEnabled(hasSelection);
            markFulfilledButton.setEnabled(hasSelection);
        });

        JScrollPane scrollPane = ModernTable.createScrollPane(requestTable);

        // Bottom action buttons
        JPanel tableBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tableBottom.setOpaque(false);

        searchMatchingButton = new ModernButton("🔍 Find Donors for Selected", ModernButton.ButtonStyle.SECONDARY);
        searchMatchingButton.setEnabled(false);
        searchMatchingButton.addActionListener(e -> navigationManager.showDonorSearchDialog(this));

        markFulfilledButton = new ModernButton("✓ Mark Fulfilled", ModernButton.ButtonStyle.PRIMARY);
        markFulfilledButton.setEnabled(false);
        markFulfilledButton.addActionListener(e -> handleMarkFulfilled());

        tableBottom.add(searchMatchingButton);
        tableBottom.add(markFulfilledButton);

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(tableBottom, BorderLayout.SOUTH);

        mainContent.add(tableCard);

        add(mainContent, BorderLayout.CENTER);
    }

    public void refreshDashboard() {
        headerPanel.refreshState();

        User user = SessionManager.getInstance().getCurrentUser();
        String myId = user != null ? user.getUserId() : "";

        NetworkManager.getInstance().getBloodRequests("", "", "")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    requestModel.setRowCount(0);
                    myRequests.clear();

                    if (response.isSuccess()) {
                        List<BloodRequest> all = ResponseHandler.parseBloodRequests(response);
                        int active = 0;
                        for (BloodRequest br : all) {
                            // Filter to user's requests if logged in, or show all in mock
                            if (myId.isEmpty() || myId.equals(br.getRecipientId()) || SessionManager.getInstance().isMockMode()) {
                                myRequests.add(br);
                                if ("OPEN".equalsIgnoreCase(br.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(br.getStatus())) {
                                    active++;
                                }
                                requestModel.addRow(new Object[]{
                                        br.getRequestId(),
                                        ValidationUtils.toDisplayBloodGroup(br.getBloodGroup()),
                                        br.getUnitsRequired() + " pints",
                                        br.getHospital(),
                                        br.getCity(),
                                        br.getUrgency(),
                                        br.getStatus()
                                });
                            }
                        }
                        activeRequestsCard.setValue(Integer.toString(active));
                    }
                }))
                .exceptionally(throwable -> null);

        // Fetch matching eligible donors count
        NetworkManager.getInstance().matchDonors("", "")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        int donors = response.getInt("count", 0);
                        matchingDonorsCard.setValue(Integer.toString(donors));
                    }
                }))
                .exceptionally(throwable -> null);
    }

    private void handleMarkFulfilled() {
        int selected = requestTable.getSelectedRow();
        if (selected >= 0 && selected < myRequests.size()) {
            BloodRequest br = myRequests.get(selected);
            NetworkManager.getInstance().updateBloodRequestStatus(br.getRequestId(), "FULFILLED")
                    .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                        if (response.isSuccess()) {
                            JOptionPane.showMessageDialog(this, "Blood request marked as FULFILLED!");
                            refreshDashboard();
                        } else {
                            JOptionPane.showMessageDialog(this, "Could not update status: " + response.getError(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }))
                    .exceptionally(throwable -> null);
        }
    }

    // Push Event Listener Implementations
    @Override
    public void onUrgentAlertReceived(String bloodGroup, String hospital, String city, String urgency, String title) {
        refreshDashboard();
    }

    @Override
    public void onIncomingMessageReceived(Message message) {
    }

    @Override
    public void onConnectionStateChanged(boolean connected, String message) {
        headerPanel.setConnectionStatus(connected);
        statusCard.setValue(connected ? "ONLINE" : "OFFLINE");
    }
}
