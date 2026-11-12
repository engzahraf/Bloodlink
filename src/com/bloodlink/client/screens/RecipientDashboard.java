package com.bloodlink.client.screens;

import com.bloodlink.client.models.BloodRequest;
import com.bloodlink.client.models.DonorResponse;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Recipient Dashboard for BloodLink.
 * Enables patients and hospital representatives to submit blood requests,
 * monitor real-time donor responses (ACCEPTED / REJECTED / PENDING),
 * view donor profiles, and communicate directly with donors.
 */
public class RecipientDashboard extends JFrame implements NetworkManager.PushEventListener {

    private final NavigationManager navigationManager;

    private HeaderPanel headerPanel;
    private StatCard activeRequestsCard;
    private StatCard matchingDonorsCard;
    private StatCard statusCard;

    private ModernTable requestTable;
    private DefaultTableModel requestModel;
    private ModernButton markFulfilledButton;
    private ModernButton viewResponsesButton;

    // Donor Responses Section
    private JLabel selectedRequestHeader;
    private ModernTable responsesTable;
    private DefaultTableModel responsesModel;
    private ModernButton messageDonorButton;
    private ModernButton viewProfileButton;
    private JLabel responsesNoticeLabel;

    private final List<BloodRequest> myRequests = new ArrayList<>();
    private final List<DonorResponse> currentResponses = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");

    public RecipientDashboard(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        initUI();
        NetworkManager.getInstance().addPushEventListener(this);
        refreshDashboard();
    }

    private void initUI() {
        setTitle("BloodLink - Recipient Dashboard & Donor Response Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setMinimumSize(new Dimension(980, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);
        setLayout(new BorderLayout());

        // Top Header
        headerPanel = new HeaderPanel(navigationManager);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content with ScrollPane
        JPanel contentContainer = new JPanel();
        contentContainer.setOpaque(false);
        contentContainer.setLayout(new BoxLayout(contentContainer, BoxLayout.Y_AXIS));
        contentContainer.setBorder(new EmptyBorder(18, 24, 20, 24));

        // 1. StatCards Row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        activeRequestsCard = new StatCard("Active Requests", "0", "Awaiting fulfillment", ThemeManager.COLOR_CRITICAL);
        matchingDonorsCard = new StatCard("Available Donors", "0", "Eligible in network", ThemeManager.COLOR_SUCCESS);
        statusCard = new StatCard("Network Status", "ONLINE", "TCP socket active", ThemeManager.COLOR_PRIMARY);

        statsRow.add(activeRequestsCard);
        statsRow.add(matchingDonorsCard);
        statsRow.add(statusCard);
        contentContainer.add(statsRow);
        contentContainer.add(Box.createVerticalStrut(18));

        // 2. Action Toolbar
        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionToolbar.setOpaque(false);
        actionToolbar.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));

        ModernButton createReqBtn = new ModernButton("+ Request Blood Now", ModernButton.ButtonStyle.PRIMARY);
        createReqBtn.setFont(ThemeManager.FONT_CAPTION);
        createReqBtn.addActionListener(e -> navigationManager.showCreateRequestDialog(this, this::refreshDashboard));

        ModernButton searchDonorsBtn = new ModernButton("🔍 Search Compatible Donors", ModernButton.ButtonStyle.SECONDARY);
        searchDonorsBtn.setFont(ThemeManager.FONT_CAPTION);
        searchDonorsBtn.addActionListener(e -> navigationManager.showDonorSearchDialog(this));

        ModernButton notifBtn = new ModernButton("🔔 View Alerts & Notifications", ModernButton.ButtonStyle.SECONDARY);
        notifBtn.setFont(ThemeManager.FONT_CAPTION);
        notifBtn.addActionListener(e -> navigationManager.showNotifications());

        ModernButton refreshBtn = new ModernButton("🔄 Refresh Status", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setFont(ThemeManager.FONT_CAPTION);
        refreshBtn.addActionListener(e -> refreshDashboard());

        actionToolbar.add(createReqBtn);
        actionToolbar.add(searchDonorsBtn);
        actionToolbar.add(notifBtn);
        actionToolbar.add(refreshBtn);
        contentContainer.add(actionToolbar);
        contentContainer.add(Box.createVerticalStrut(16));

        // 3. My Requests Table Card
        JPanel tableCard = new JPanel(new BorderLayout(0, 10));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(ThemeManager.createCardBorder());
        tableCard.setPreferredSize(new Dimension(Short.MAX_VALUE, 260));

        JLabel tableTitle = new JLabel("My Submitted Blood Requests");
        tableTitle.setFont(ThemeManager.FONT_TITLE);
        tableTitle.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        String[] cols = {"Request ID", "Required Blood", "Units", "Hospital", "City", "Urgency", "Status", "Donor Responses"};
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
            markFulfilledButton.setEnabled(hasSelection);
            viewResponsesButton.setEnabled(hasSelection);
            if (hasSelection) {
                loadResponsesForSelectedRequest();
            }
        });

        JScrollPane scrollPane = ModernTable.createScrollPane(requestTable);

        JPanel tableBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tableBottom.setOpaque(false);

        viewResponsesButton = new ModernButton("👁 View Donor Responses", ModernButton.ButtonStyle.SECONDARY);
        viewResponsesButton.setEnabled(false);
        viewResponsesButton.addActionListener(e -> loadResponsesForSelectedRequest());

        markFulfilledButton = new ModernButton("✓ Mark Request Fulfilled", ModernButton.ButtonStyle.PRIMARY);
        markFulfilledButton.setEnabled(false);
        markFulfilledButton.addActionListener(e -> handleMarkFulfilled());

        tableBottom.add(viewResponsesButton);
        tableBottom.add(markFulfilledButton);

        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        tableCard.add(tableBottom, BorderLayout.SOUTH);

        contentContainer.add(tableCard);
        contentContainer.add(Box.createVerticalStrut(16));

        // 4. Donor Responses Detail Card
        JPanel responsesCard = new JPanel(new BorderLayout(0, 10));
        responsesCard.setBackground(Color.WHITE);
        responsesCard.setBorder(ThemeManager.createCardBorder());
        responsesCard.setPreferredSize(new Dimension(Short.MAX_VALUE, 280));

        JPanel responsesHeaderPanel = new JPanel(new BorderLayout());
        responsesHeaderPanel.setOpaque(false);

        selectedRequestHeader = new JLabel("Donor Responses (Select a Blood Request above)");
        selectedRequestHeader.setFont(ThemeManager.FONT_TITLE);
        selectedRequestHeader.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        responsesNoticeLabel = new JLabel(" ");
        responsesNoticeLabel.setFont(ThemeManager.FONT_CAPTION);
        responsesNoticeLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        responsesHeaderPanel.add(selectedRequestHeader, BorderLayout.NORTH);
        responsesHeaderPanel.add(responsesNoticeLabel, BorderLayout.SOUTH);

        String[] respCols = {"Donor Name", "Blood Group", "Donor City", "Availability", "Response Status", "Responded At"};
        responsesModel = new DefaultTableModel(respCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        responsesTable = new ModernTable(responsesModel);
        responsesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        responsesTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasRespSelection = responsesTable.getSelectedRow() >= 0;
            messageDonorButton.setEnabled(hasRespSelection);
            viewProfileButton.setEnabled(hasRespSelection);
        });

        JScrollPane respScrollPane = ModernTable.createScrollPane(responsesTable);

        JPanel respBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        respBottom.setOpaque(false);

        viewProfileButton = new ModernButton("👤 View Profile", ModernButton.ButtonStyle.SECONDARY);
        viewProfileButton.setEnabled(false);
        viewProfileButton.addActionListener(e -> handleViewDonorProfile());

        messageDonorButton = new ModernButton("💬 Message Donor", ModernButton.ButtonStyle.PRIMARY);
        messageDonorButton.setEnabled(false);
        messageDonorButton.addActionListener(e -> handleMessageDonor());

        respBottom.add(viewProfileButton);
        respBottom.add(messageDonorButton);

        responsesCard.add(responsesHeaderPanel, BorderLayout.NORTH);
        responsesCard.add(respScrollPane, BorderLayout.CENTER);
        responsesCard.add(respBottom, BorderLayout.SOUTH);

        contentContainer.add(responsesCard);

        JScrollPane mainScrollPane = new JScrollPane(contentContainer);
        mainScrollPane.setBorder(null);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScrollPane, BorderLayout.CENTER);
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
                            if (myId.isEmpty() || myId.equals(br.getRecipientId()) || SessionManager.getInstance().isMockMode()) {
                                myRequests.add(br);
                                String st = br.getStatus();
                                if ("PENDING".equalsIgnoreCase(st) || "OPEN".equalsIgnoreCase(st) || "ACCEPTED".equalsIgnoreCase(st) || "MATCHED".equalsIgnoreCase(st)) {
                                    active++;
                                }
                                requestModel.addRow(new Object[]{
                                        br.getRequestId(),
                                        ValidationUtils.toDisplayBloodGroup(br.getBloodGroup()),
                                        br.getUnitsRequired() + " pints",
                                        br.getHospital(),
                                        br.getCity(),
                                        br.getUrgency(),
                                        br.getStatus(),
                                        br.getResponseCount() + " response(s)"
                                });
                            }
                        }
                        activeRequestsCard.setValue(Integer.toString(active));

                        // Refresh response table if row still selected
                        int selected = requestTable.getSelectedRow();
                        if (selected >= 0 && selected < myRequests.size()) {
                            loadResponsesForSelectedRequest();
                        }
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

    private void loadResponsesForSelectedRequest() {
        int selected = requestTable.getSelectedRow();
        if (selected < 0 || selected >= myRequests.size()) {
            return;
        }

        BloodRequest br = myRequests.get(selected);
        selectedRequestHeader.setText("Donor Responses for Blood Request #" + br.getRequestId() +
                " • Required: " + ValidationUtils.toDisplayBloodGroup(br.getBloodGroup()) +
                " (" + br.getUnitsRequired() + " pints) • Status: " + br.getStatus());

        responsesNoticeLabel.setText("Querying server for matching donor responses...");

        NetworkManager.getInstance().getRequestResponses(br.getRequestId(), "")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    responsesModel.setRowCount(0);
                    currentResponses.clear();
                    messageDonorButton.setEnabled(false);
                    viewProfileButton.setEnabled(false);

                    if (response.isSuccess()) {
                        currentResponses.addAll(ResponseHandler.parseDonorResponses(response));
                        int acceptedCount = 0;
                        int rejectedCount = 0;

                        for (DonorResponse dr : currentResponses) {
                            if (dr.isAccepted()) acceptedCount++;
                            if (dr.isRejected()) rejectedCount++;

                            String dispStatus;
                            if (dr.isAccepted()) {
                                dispStatus = "DONOR ACCEPTED";
                            } else if (dr.isRejected()) {
                                dispStatus = "DONOR REJECTED";
                            } else {
                                dispStatus = "DONOR PENDING";
                            }

                            String respTime = dr.getCreatedAt() > 0
                                    ? dateFormat.format(new Date(dr.getCreatedAt()))
                                    : "Recent";

                            responsesModel.addRow(new Object[]{
                                    dr.getDonorName(),
                                    ValidationUtils.toDisplayBloodGroup(dr.getDonorBloodGroup()),
                                    dr.getDonorCity(),
                                    dr.getDonorAvailability(),
                                    dispStatus,
                                    respTime
                            });
                        }

                        if (currentResponses.isEmpty()) {
                            responsesNoticeLabel.setText("No donors have responded to Request #" + br.getRequestId() + " yet. Compatible donors have been alerted.");
                        } else {
                            responsesNoticeLabel.setText(currentResponses.size() + " response(s) received (" +
                                    acceptedCount + " ACCEPTED, " + rejectedCount + " REJECTED).");
                        }
                    } else {
                        responsesNoticeLabel.setText("Could not load responses: " + response.getError());
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> responsesNoticeLabel.setText("Network error loading responses: " + throwable.getMessage()));
                    return null;
                });
    }

    private void handleMessageDonor() {
        int selected = responsesTable.getSelectedRow();
        if (selected >= 0 && selected < currentResponses.size()) {
            DonorResponse dr = currentResponses.get(selected);
            navigationManager.showMessagingDialog(this, dr.getDonorId(), dr.getDonorName());
        }
    }

    private void handleViewDonorProfile() {
        int selected = responsesTable.getSelectedRow();
        if (selected >= 0 && selected < currentResponses.size()) {
            DonorResponse dr = currentResponses.get(selected);
            navigationManager.showViewDonorProfileDialog(
                    this,
                    dr.getDonorId(),
                    dr.getDonorName(),
                    dr.getDonorBloodGroup(),
                    dr.getDonorCity(),
                    dr.getDonorPhone(),
                    dr.getDonorEmail(),
                    dr.getDonorAvailability(),
                    dr.getLastDonationDate()
            );
        }
    }

    private void handleMarkFulfilled() {
        int selected = requestTable.getSelectedRow();
        if (selected >= 0 && selected < myRequests.size()) {
            BloodRequest br = myRequests.get(selected);
            NetworkManager.getInstance().updateBloodRequestStatus(br.getRequestId(), "FULFILLED")
                    .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                        if (response.isSuccess()) {
                            JOptionPane.showMessageDialog(this, "Blood request #" + br.getRequestId() + " marked as FULFILLED!");
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
        SwingUtilities.invokeLater(this::refreshDashboard);
    }

    @Override
    public void onIncomingMessageReceived(Message message) {
    }

    @Override
    public void onConnectionStateChanged(boolean connected, String message) {
        SwingUtilities.invokeLater(() -> {
            headerPanel.setConnectionStatus(connected);
            statusCard.setValue(connected ? "ONLINE" : "OFFLINE");
        });
    }
}
