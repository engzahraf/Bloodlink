package com.bloodlink.client.screens;

import com.bloodlink.client.models.BloodRequest;
import com.bloodlink.client.models.DonorResponse;
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
import java.util.Map;

/**
 * Administrator Dashboard for BloodLink.
 * Displays global system metrics, live blood group breakdown,
 * comprehensive user management (block/unblock/delete), and blood request oversight.
 */
public class AdminDashboard extends JFrame {

    private final NavigationManager navigationManager;

    private HeaderPanel headerPanel;
    private StatCard usersCard;
    private StatCard donorsCard;
    private StatCard recipientsCard;
    private StatCard requestsCard;

    // Blood Group Inventory & Demand Bar
    private JPanel bloodGroupPanel;

    // User Registry Tab
    private ModernTextField userSearchField;
    private ModernTable userTable;
    private DefaultTableModel userModel;
    private ModernButton blockUserButton;
    private ModernButton unblockUserButton;
    private ModernButton deleteUserButton;
    private ModernButton viewUserProfileButton;

    // Request Management Tab
    private ModernTable requestTable;
    private DefaultTableModel requestModel;
    private ModernButton viewResponsesButton;
    private ModernButton cancelRequestButton;
    private ModernButton contactRequesterButton;

    private final List<BloodRequest> requestList = new ArrayList<>();
    private final List<User> userList = new ArrayList<>();

    public AdminDashboard(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        initUI();
        refreshAllData();
    }

    private void initUI() {
        setTitle("BloodLink - Central Administration & Registry Control Console");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1140, 820);
        setMinimumSize(new Dimension(1000, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(ThemeManager.COLOR_BACKGROUND);
        setLayout(new BorderLayout());

        // Header Panel
        headerPanel = new HeaderPanel(navigationManager);
        add(headerPanel, BorderLayout.NORTH);

        // Main Scrollable Dashboard Content
        JPanel mainContent = new JPanel();
        mainContent.setOpaque(false);
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBorder(new EmptyBorder(18, 24, 24, 24));

        // 1. Metric StatCards Row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        usersCard = new StatCard("Total Accounts", "0", "All registered users", ThemeManager.COLOR_PRIMARY);
        donorsCard = new StatCard("Registered Donors", "0", "Available life-savers", ThemeManager.COLOR_SUCCESS);
        recipientsCard = new StatCard("Recipients / Clinics", "0", "Awaiting fulfillment", ThemeManager.COLOR_WARNING);
        requestsCard = new StatCard("Blood Requests", "0", "0 active / open", ThemeManager.COLOR_CRITICAL);

        statsRow.add(usersCard);
        statsRow.add(donorsCard);
        statsRow.add(recipientsCard);
        statsRow.add(requestsCard);
        mainContent.add(statsRow);
        mainContent.add(Box.createVerticalStrut(16));

        // 2. Blood Group Breakdown Bar
        bloodGroupPanel = new JPanel(new GridLayout(1, 8, 10, 0));
        bloodGroupPanel.setOpaque(true);
        bloodGroupPanel.setBackground(Color.WHITE);
        bloodGroupPanel.setBorder(BorderFactory.createCompoundBorder(
                ThemeManager.createCardBorder(),
                new EmptyBorder(10, 14, 10, 14)
        ));
        bloodGroupPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, 65));
        populateDefaultBloodGroupCards();
        mainContent.add(bloodGroupPanel);
        mainContent.add(Box.createVerticalStrut(16));

        // 3. Quick Actions Toolbar
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionsRow.setOpaque(false);
        actionsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));

        ModernButton createReqBtn = new ModernButton("+ Broadcast Blood Request", ModernButton.ButtonStyle.PRIMARY);
        createReqBtn.setFont(ThemeManager.FONT_CAPTION);
        createReqBtn.addActionListener(e -> navigationManager.showCreateRequestDialog(this, this::refreshAllData));

        ModernButton searchDonorsBtn = new ModernButton("🔍 Search Donors Network", ModernButton.ButtonStyle.SECONDARY);
        searchDonorsBtn.setFont(ThemeManager.FONT_CAPTION);
        searchDonorsBtn.addActionListener(e -> navigationManager.showDonorSearchDialog(this));

        ModernButton notifBtn = new ModernButton("🔔 System Notifications", ModernButton.ButtonStyle.SECONDARY);
        notifBtn.setFont(ThemeManager.FONT_CAPTION);
        notifBtn.addActionListener(e -> navigationManager.showNotifications());

        ModernButton refreshBtn = new ModernButton("🔄 Refresh Database", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setFont(ThemeManager.FONT_CAPTION);
        refreshBtn.addActionListener(e -> refreshAllData());

        actionsRow.add(createReqBtn);
        actionsRow.add(searchDonorsBtn);
        actionsRow.add(notifBtn);
        actionsRow.add(refreshBtn);
        mainContent.add(actionsRow);
        mainContent.add(Box.createVerticalStrut(16));

        // 4. Tabbed Pane: User Management & Request Oversight
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ThemeManager.FONT_BODY_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: User Management Panel
        JPanel userTabPanel = createUsersTab();
        tabbedPane.addTab("User Registry & Authorization", userTabPanel);

        // Tab 2: Blood Requests Panel
        JPanel reqTabPanel = createRequestsTab();
        tabbedPane.addTab("Global Blood Requests Oversight", reqTabPanel);

        mainContent.add(tabbedPane);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void populateDefaultBloodGroupCards() {
        bloodGroupPanel.removeAll();
        String[] groups = {"O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"};
        for (String g : groups) {
            JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
            card.setOpaque(false);
            JLabel title = new JLabel(g, SwingConstants.CENTER);
            title.setFont(ThemeManager.FONT_BODY_BOLD);
            title.setForeground(ThemeManager.COLOR_PRIMARY);

            JLabel count = new JLabel("0 req", SwingConstants.CENTER);
            count.setFont(ThemeManager.FONT_CAPTION);
            count.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

            card.add(title);
            card.add(count);
            bloodGroupPanel.add(card);
        }
        bloodGroupPanel.revalidate();
        bloodGroupPanel.repaint();
    }

    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Search Bar Row
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);

        userSearchField = new ModernTextField("", "Search users by name, email, phone, or blood group...", 35);
        userSearchField.addActionListener(e -> loadUsersData(userSearchField.getText().trim()));

        ModernButton searchBtn = new ModernButton("🔍 Search", ModernButton.ButtonStyle.SECONDARY);
        searchBtn.addActionListener(e -> loadUsersData(userSearchField.getText().trim()));

        ModernButton clearBtn = new ModernButton("Clear", ModernButton.ButtonStyle.OUTLINE);
        clearBtn.addActionListener(e -> {
            userSearchField.setText("");
            loadUsersData("");
        });

        searchBar.add(new JLabel("Find Accounts:"));
        searchBar.add(userSearchField);
        searchBar.add(searchBtn);
        searchBar.add(clearBtn);

        panel.add(searchBar, BorderLayout.NORTH);

        // Users Table
        String[] userCols = {"User ID", "Full Name", "Email / Account", "Role", "Blood Group", "City", "Phone", "Status"};
        userModel = new DefaultTableModel(userCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        userTable = new ModernTable(userModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getSelectionModel().addListSelectionListener(e -> updateUserActionButtons());

        JScrollPane userScroll = ModernTable.createScrollPane(userTable);
        panel.add(userScroll, BorderLayout.CENTER);

        // Bottom Actions
        JPanel userBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        userBottom.setOpaque(false);

        viewUserProfileButton = new ModernButton("👤 View Profile", ModernButton.ButtonStyle.SECONDARY);
        viewUserProfileButton.setEnabled(false);
        viewUserProfileButton.addActionListener(e -> handleViewUserProfile());

        blockUserButton = new ModernButton("🚫 Block Account", ModernButton.ButtonStyle.SECONDARY);
        blockUserButton.setEnabled(false);
        blockUserButton.addActionListener(e -> handleBlockUser());

        unblockUserButton = new ModernButton("✓ Unblock Account", ModernButton.ButtonStyle.PRIMARY);
        unblockUserButton.setEnabled(false);
        unblockUserButton.addActionListener(e -> handleUnblockUser());

        deleteUserButton = new ModernButton("🗑 Delete Account", ModernButton.ButtonStyle.OUTLINE);
        deleteUserButton.setEnabled(false);
        deleteUserButton.addActionListener(e -> handleDeleteUser());

        userBottom.add(viewUserProfileButton);
        userBottom.add(blockUserButton);
        userBottom.add(unblockUserButton);
        userBottom.add(deleteUserButton);

        panel.add(userBottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createRequestsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] reqCols = {"Request ID", "Required Blood", "Units", "Hospital", "City", "Urgency", "Status", "Responses"};
        requestModel = new DefaultTableModel(reqCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestTable = new ModernTable(requestModel);
        requestTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestTable.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSel = requestTable.getSelectedRow() >= 0;
            viewResponsesButton.setEnabled(hasSel);
            cancelRequestButton.setEnabled(hasSel);
            contactRequesterButton.setEnabled(hasSel);
        });

        JScrollPane reqScroll = ModernTable.createScrollPane(requestTable);
        panel.add(reqScroll, BorderLayout.CENTER);

        JPanel reqBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        reqBottom.setOpaque(false);

        viewResponsesButton = new ModernButton("👁 View Donor Responses", ModernButton.ButtonStyle.SECONDARY);
        viewResponsesButton.setEnabled(false);
        viewResponsesButton.addActionListener(e -> handleViewDonorResponsesForRequest());

        contactRequesterButton = new ModernButton("💬 Message Requester", ModernButton.ButtonStyle.SECONDARY);
        contactRequesterButton.setEnabled(false);
        contactRequesterButton.addActionListener(e -> {
            int row = requestTable.getSelectedRow();
            if (row >= 0 && row < requestList.size()) {
                BloodRequest br = requestList.get(row);
                navigationManager.showMessagingDialog(this, br.getRecipientId(), br.getHospital());
            }
        });

        cancelRequestButton = new ModernButton("❌ Cancel Request", ModernButton.ButtonStyle.OUTLINE);
        cancelRequestButton.setEnabled(false);
        cancelRequestButton.addActionListener(e -> handleCancelRequest());

        reqBottom.add(viewResponsesButton);
        reqBottom.add(contactRequesterButton);
        reqBottom.add(cancelRequestButton);

        panel.add(reqBottom, BorderLayout.SOUTH);
        return panel;
    }

    private void updateUserActionButtons() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= userList.size()) {
            blockUserButton.setEnabled(false);
            unblockUserButton.setEnabled(false);
            deleteUserButton.setEnabled(false);
            viewUserProfileButton.setEnabled(false);
            return;
        }

        User u = userList.get(row);
        viewUserProfileButton.setEnabled(true);
        deleteUserButton.setEnabled(true);

        String currentUserId = SessionManager.getInstance().getCurrentUserId();
        boolean isSelf = u.getUserId().equals(currentUserId);

        if (isSelf) {
            blockUserButton.setEnabled(false);
            unblockUserButton.setEnabled(false);
            deleteUserButton.setEnabled(false);
            return;
        }

        Object statusObj = userModel.getValueAt(row, 7);
        String st = statusObj != null ? statusObj.toString() : "ACTIVE";
        if ("BLOCKED".equalsIgnoreCase(st)) {
            blockUserButton.setEnabled(false);
            unblockUserButton.setEnabled(true);
        } else {
            blockUserButton.setEnabled(true);
            unblockUserButton.setEnabled(false);
        }
    }

    public void refreshAllData() {
        headerPanel.refreshState();

        // 1. Fetch system statistics
        NetworkManager.getInstance().adminGetStats()
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        Map<String, String> stats = response.getPayload();
                        usersCard.setValue(stats.getOrDefault("totalUsers", "0"));
                        usersCard.setSubtitle(stats.getOrDefault("totalBlocked", "0") + " blocked");

                        donorsCard.setValue(stats.getOrDefault("totalDonors", "0"));
                        recipientsCard.setValue(stats.getOrDefault("totalRecipients", "0"));

                        requestsCard.setValue(stats.getOrDefault("totalRequests", "0"));
                        requestsCard.setSubtitle(stats.getOrDefault("pendingRequests", "0") + " pending, " +
                                stats.getOrDefault("acceptedRequests", "0") + " accepted");

                        // Update blood group distribution panel
                        updateBloodGroupCards(stats);
                    }
                }))
                .exceptionally(throwable -> null);

        // 2. Fetch blood requests
        NetworkManager.getInstance().getBloodRequests("", "", "")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    requestModel.setRowCount(0);
                    requestList.clear();

                    if (response.isSuccess()) {
                        requestList.addAll(ResponseHandler.parseBloodRequests(response));
                        for (BloodRequest br : requestList) {
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
                }))
                .exceptionally(throwable -> null);

        // 3. Fetch users
        loadUsersData("");
    }

    private void updateBloodGroupCards(Map<String, String> stats) {
        bloodGroupPanel.removeAll();
        String[] groups = {"O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"};
        for (String g : groups) {
            String norm = ValidationUtils.toCanonicalWireBloodGroup(g);
            String count = stats.getOrDefault("bg_" + norm, stats.getOrDefault("bgSymbol_" + g, "0"));

            JPanel card = new JPanel(new GridLayout(2, 1, 0, 2));
            card.setOpaque(false);
            JLabel title = new JLabel(g, SwingConstants.CENTER);
            title.setFont(ThemeManager.FONT_BODY_BOLD);
            title.setForeground(ThemeManager.COLOR_PRIMARY);

            JLabel cntLabel = new JLabel(count + " req", SwingConstants.CENTER);
            cntLabel.setFont(ThemeManager.FONT_CAPTION);
            cntLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

            card.add(title);
            card.add(cntLabel);
            bloodGroupPanel.add(card);
        }
        bloodGroupPanel.revalidate();
        bloodGroupPanel.repaint();
    }

    private void loadUsersData(String search) {
        NetworkManager.getInstance().adminGetUsers(search)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    userModel.setRowCount(0);
                    userList.clear();

                    if (response.isSuccess()) {
                        userList.addAll(ResponseHandler.parseAdminUsers(response));
                        for (User u : userList) {
                            userModel.addRow(new Object[]{
                                    u.getUserId(),
                                    u.getFullName(),
                                    u.getEmail(),
                                    u.getRole(),
                                    ValidationUtils.toDisplayBloodGroup(u.getBloodGroup()),
                                    u.getCity() != null ? u.getCity() : "",
                                    u.getPhone() != null ? u.getPhone() : "",
                                    "ACTIVE" // default unless blocked
                            });
                        }
                    }
                    updateUserActionButtons();
                }))
                .exceptionally(throwable -> null);
    }

    private void handleBlockUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= userList.size()) return;

        User u = userList.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Block user " + u.getFullName() + " (" + u.getEmail() + ")?\nThey will be prevented from logging in.",
                "Confirm Block",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        NetworkManager.getInstance().adminBlockUser(u.getUserId())
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        userModel.setValueAt("BLOCKED", row, 7);
                        updateUserActionButtons();
                        JOptionPane.showMessageDialog(this, "User " + u.getFullName() + " has been BLOCKED.", "User Blocked", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to block user: " + response.getError(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Network error: " + throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                    return null;
                });
    }

    private void handleUnblockUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= userList.size()) return;

        User u = userList.get(row);
        NetworkManager.getInstance().adminUnblockUser(u.getUserId())
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        userModel.setValueAt("ACTIVE", row, 7);
                        updateUserActionButtons();
                        JOptionPane.showMessageDialog(this, "User " + u.getFullName() + " has been UNBLOCKED and activated.", "User Unblocked", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to unblock user: " + response.getError(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Network error: " + throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                    return null;
                });
    }

    private void handleDeleteUser() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= userList.size()) return;

        User u = userList.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "PERMANENT ACTION: Delete user " + u.getFullName() + " (" + u.getEmail() + ")?\nAll linked records will be removed.",
                "Confirm Account Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        NetworkManager.getInstance().adminDeleteUser(u.getUserId())
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(this, "User " + u.getFullName() + " deleted successfully.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                        refreshAllData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to delete user: " + response.getError(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Network error: " + throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                    return null;
                });
    }

    private void handleCancelRequest() {
        int row = requestTable.getSelectedRow();
        if (row < 0 || row >= requestList.size()) return;

        BloodRequest br = requestList.get(row);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Cancel Blood Request #" + br.getRequestId() + " (" + br.getHospital() + ")?",
                "Confirm Cancellation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        NetworkManager.getInstance().adminCancelRequest(br.getRequestId())
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        requestModel.setValueAt("CANCELLED", row, 6);
                        JOptionPane.showMessageDialog(this, "Blood request #" + br.getRequestId() + " has been CANCELLED.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                        refreshAllData();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to cancel request: " + response.getError(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Network error: " + throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                    return null;
                });
    }

    private void handleViewUserProfile() {
        int row = userTable.getSelectedRow();
        if (row < 0 || row >= userList.size()) return;

        User u = userList.get(row);
        navigationManager.showViewDonorProfileDialog(
                this,
                u.getUserId(),
                u.getFullName(),
                u.getBloodGroup(),
                u.getCity(),
                u.getPhone(),
                u.getEmail(),
                "ACTIVE",
                "Never"
        );
    }

    private void handleViewDonorResponsesForRequest() {
        int row = requestTable.getSelectedRow();
        if (row < 0 || row >= requestList.size()) return;

        BloodRequest br = requestList.get(row);
        JDialog dialog = new JDialog(this, "Donor Responses for Request #" + br.getRequestId(), true);
        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Donor Responses for Blood Request #" + br.getRequestId() + " (" +
                ValidationUtils.toDisplayBloodGroup(br.getBloodGroup()) + " at " + br.getHospital() + ")");
        title.setFont(ThemeManager.FONT_TITLE);
        title.setForeground(ThemeManager.COLOR_PRIMARY);

        String[] cols = {"Donor Name", "Blood Group", "Donor City", "Phone", "Email", "Response Status", "Responded At"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        ModernTable tbl = new ModernTable(model);
        JScrollPane sp = ModernTable.createScrollPane(tbl);

        JLabel statusLbl = new JLabel("Loading responses from server...");
        statusLbl.setFont(ThemeManager.FONT_CAPTION);
        statusLbl.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        ModernButton closeBtn = new ModernButton("Close", ModernButton.ButtonStyle.PRIMARY);
        closeBtn.addActionListener(e -> dialog.dispose());
        bottom.add(statusLbl, BorderLayout.WEST);
        bottom.add(closeBtn, BorderLayout.EAST);

        p.add(title, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(p);
        dialog.pack();
        dialog.setSize(820, 480);
        dialog.setLocationRelativeTo(this);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        NetworkManager.getInstance().getRequestResponses(br.getRequestId(), "")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        List<DonorResponse> responses = ResponseHandler.parseDonorResponses(response);
                        for (DonorResponse dr : responses) {
                            String st = dr.isAccepted() ? "DONOR ACCEPTED" : (dr.isRejected() ? "DONOR REJECTED" : "DONOR PENDING");
                            String ts = dr.getCreatedAt() > 0 ? sdf.format(new Date(dr.getCreatedAt())) : "Recent";
                            model.addRow(new Object[]{
                                    dr.getDonorName(),
                                    ValidationUtils.toDisplayBloodGroup(dr.getDonorBloodGroup()),
                                    dr.getDonorCity(),
                                    dr.getDonorPhone(),
                                    dr.getDonorEmail(),
                                    st,
                                    ts
                            });
                        }
                        statusLbl.setText(responses.size() + " donor response(s) recorded in database.");
                    } else {
                        statusLbl.setText("No responses found: " + response.getError());
                    }
                }))
                .exceptionally(throwable -> null);

        dialog.setVisible(true);
    }
}
