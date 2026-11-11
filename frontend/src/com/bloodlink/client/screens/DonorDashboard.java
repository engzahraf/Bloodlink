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
 * Donor Dashboard.
 * Displays donor profile summary, availability toggle, and matching emergency blood requests.
 */
public class DonorDashboard extends JFrame implements NetworkManager.PushEventListener {

    private final NavigationManager navigationManager;

    private HeaderPanel headerPanel;
    private StatCard bloodGroupCard;
    private StatCard matchingRequestsCard;
    private StatCard availabilityCard;

    private ModernTable requestsTable;
    private DefaultTableModel requestsModel;
    private ModernButton contactButton;
    private JLabel bannerAlertLabel;

    private final List<BloodRequest> matchingRequests = new ArrayList<>();

    public DonorDashboard(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        initUI();
        NetworkManager.getInstance().addPushEventListener(this);
        refreshDashboard();
    }

    private void initUI() {
        setTitle("BloodLink - Donor Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 720);
        setMinimumSize(new Dimension(920, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);
        setLayout(new BorderLayout());

        // Header
        headerPanel = new HeaderPanel(navigationManager);
        add(headerPanel, BorderLayout.NORTH);

        // Main content
        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(new EmptyBorder(18, 24, 20, 24));

        // Urgent Alert Banner (shown when push alert arrives)
        bannerAlertLabel = new JLabel(" ");
        bannerAlertLabel.setFont(ThemeManager.FONT_BODY_BOLD);
        bannerAlertLabel.setForeground(ThemeManager.COLOR_CRITICAL);
        bannerAlertLabel.setVisible(false);
        mainContent.add(bannerAlertLabel);

        // Stats Row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        User user = SessionManager.getInstance().getCurrentUser();
        String displayBg = user != null ? ValidationUtils.toDisplayBloodGroup(user.getBloodGroup()) : "O+";

        bloodGroupCard = new StatCard("My Blood Group", displayBg, "Universal recipient compatibility", ThemeManager.COLOR_PRIMARY);
        matchingRequestsCard = new StatCard("Matching Blood Drives", "0", "Awaiting compatible donation", ThemeManager.COLOR_CRITICAL);
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

        ModernButton refreshBtn = new ModernButton("🔄 Refresh Matching Drives", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setFont(ThemeManager.FONT_CAPTION);
        refreshBtn.addActionListener(e -> refreshDashboard());

        ModernButton searchDonorsBtn = new ModernButton("🔍 Browse Network", ModernButton.ButtonStyle.SECONDARY);
        searchDonorsBtn.setFont(ThemeManager.FONT_CAPTION);
        searchDonorsBtn.addActionListener(e -> navigationManager.showDonorSearchDialog(this));

        actionToolbar.add(editProfileBtn);
        actionToolbar.add(refreshBtn);
        actionToolbar.add(searchDonorsBtn);
        mainContent.add(actionToolbar);
        mainContent.add(Box.createVerticalStrut(16));

        // Matching Requests Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(ThemeManager.createCardBorder());

        JLabel tableTitle = new JLabel("Matching Blood Donation Requests");
        tableTitle.setFont(ThemeManager.FONT_TITLE);
        tableTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        String[] cols = {"Request ID", "Required Blood", "Units", "Hospital", "City", "Urgency", "Status"};
        requestsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestsTable = new ModernTable(requestsModel);
        requestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestsTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = requestsTable.getSelectedRow() >= 0;
            contactButton.setEnabled(hasSelection);
        });

        JScrollPane scrollPane = ModernTable.createScrollPane(requestsTable);

        // Table bottom action row
        JPanel tableBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tableBottom.setOpaque(false);

        contactButton = new ModernButton("💬 Message Recipient / Hospital", ModernButton.ButtonStyle.PRIMARY);
        contactButton.setEnabled(false);
        contactButton.addActionListener(e -> {
            int row = requestsTable.getSelectedRow();
            if (row >= 0 && row < matchingRequests.size()) {
                BloodRequest br = matchingRequests.get(row);
                navigationManager.showMessagingDialog(this, br.getRecipientId(), br.getHospital());
            }
        });

        tableBottom.add(contactButton);

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(tableBottom, BorderLayout.SOUTH);

        mainContent.add(tableCard);

        add(mainContent, BorderLayout.CENTER);
    }

    public void refreshDashboard() {
        headerPanel.refreshState();

        User user = SessionManager.getInstance().getCurrentUser();
        String myBg = user != null ? ValidationUtils.toCanonicalWireBloodGroup(user.getBloodGroup()) : "";
        String displayBg = user != null ? ValidationUtils.toDisplayBloodGroup(user.getBloodGroup()) : "O+";
        bloodGroupCard.setValue(displayBg);

        NetworkManager.getInstance().getBloodRequests("OPEN", myBg, "")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    requestsModel.setRowCount(0);
                    matchingRequests.clear();

                    if (response.isSuccess()) {
                        matchingRequests.addAll(ResponseHandler.parseBloodRequests(response));
                        for (BloodRequest br : matchingRequests) {
                            requestsModel.addRow(new Object[]{
                                    br.getRequestId(),
                                    ValidationUtils.toDisplayBloodGroup(br.getBloodGroup()),
                                    br.getUnitsRequired() + " pints",
                                    br.getHospital(),
                                    br.getCity(),
                                    br.getUrgency(),
                                    br.getStatus()
                            });
                        }
                        matchingRequestsCard.setValue(Integer.toString(matchingRequests.size()));
                    }
                }))
                .exceptionally(throwable -> null);
    }

    // Push Event Listener Implementations
    @Override
    public void onUrgentAlertReceived(String bloodGroup, String hospital, String city, String urgency, String title) {
        bannerAlertLabel.setText("🚨 " + (title != null ? title : "URGENT BLOOD ALERT") + " at " + hospital + " (" + bloodGroup + ")");
        bannerAlertLabel.setVisible(true);
        refreshDashboard();
    }

    @Override
    public void onIncomingMessageReceived(Message message) {
        // Automatically reflected when messaging dialog opens
    }

    @Override
    public void onConnectionStateChanged(boolean connected, String message) {
        headerPanel.setConnectionStatus(connected);
    }
}
