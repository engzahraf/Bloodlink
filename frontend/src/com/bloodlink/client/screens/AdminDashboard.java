package com.bloodlink.client.screens;

import com.bloodlink.client.models.BloodRequest;
import com.bloodlink.client.models.User;
import com.bloodlink.client.network.MockDataService;
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
 * Administrator Dashboard for BloodLink.
 * Displays global system metrics, user registry oversight, and active blood requests management.
 */
public class AdminDashboard extends JFrame {

    private final NavigationManager navigationManager;

    private HeaderPanel headerPanel;
    private StatCard usersCard;
    private StatCard donorsCard;
    private StatCard recipientsCard;
    private StatCard requestsCard;

    private ModernTable userTable;
    private DefaultTableModel userModel;
    private ModernTable requestTable;
    private DefaultTableModel requestModel;

    private final List<BloodRequest> requestList = new ArrayList<>();

    public AdminDashboard(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        initUI();
        refreshAllData();
    }

    private void initUI() {
        setTitle("BloodLink - Administrator Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 750);
        setMinimumSize(new Dimension(960, 680));
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
        mainContent.setBorder(new EmptyBorder(20, 24, 24, 24));

        // Metric StatCards Row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        statsRow.setOpaque(false);
        statsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 110));

        usersCard = new StatCard("Total Registered", "0", "All system accounts", ThemeManager.COLOR_PRIMARY);
        donorsCard = new StatCard("Registered Donors", "0", "Available life-savers", ThemeManager.COLOR_SUCCESS);
        recipientsCard = new StatCard("Recipients", "0", "Patients & clinics", ThemeManager.COLOR_WARNING);
        requestsCard = new StatCard("Blood Requests", "0", "Open blood drives", ThemeManager.COLOR_CRITICAL);

        statsRow.add(usersCard);
        statsRow.add(donorsCard);
        statsRow.add(recipientsCard);
        statsRow.add(requestsCard);
        mainContent.add(statsRow);
        mainContent.add(Box.createVerticalStrut(20));

        // Quick Actions Row
        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actionsRow.setOpaque(false);
        actionsRow.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));

        ModernButton createReqBtn = new ModernButton("+ Broadcast Blood Request", ModernButton.ButtonStyle.PRIMARY);
        createReqBtn.setFont(ThemeManager.FONT_CAPTION);
        createReqBtn.addActionListener(e -> navigationManager.showCreateRequestDialog(this, this::refreshAllData));

        ModernButton searchDonorsBtn = new ModernButton("🔍 Search Donors", ModernButton.ButtonStyle.SECONDARY);
        searchDonorsBtn.setFont(ThemeManager.FONT_CAPTION);
        searchDonorsBtn.addActionListener(e -> navigationManager.showDonorSearchDialog(this));

        ModernButton refreshBtn = new ModernButton("🔄 Refresh Tables", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setFont(ThemeManager.FONT_CAPTION);
        refreshBtn.addActionListener(e -> refreshAllData());

        actionsRow.add(createReqBtn);
        actionsRow.add(searchDonorsBtn);
        actionsRow.add(refreshBtn);
        mainContent.add(actionsRow);
        mainContent.add(Box.createVerticalStrut(16));

        // Tabbed Panel: User Management & Request Management
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(ThemeManager.FONT_BODY_BOLD);
        tabbedPane.setBackground(Color.WHITE);

        // Tab 1: Blood Requests Table
        String[] reqCols = {"Request ID", "Blood Group", "Units", "Hospital", "City", "Urgency", "Status"};
        requestModel = new DefaultTableModel(reqCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        requestTable = new ModernTable(requestModel);
        JScrollPane reqScroll = ModernTable.createScrollPane(requestTable);

        JPanel reqTabPanel = new JPanel(new BorderLayout(0, 8));
        reqTabPanel.setOpaque(true);
        reqTabPanel.setBackground(Color.WHITE);
        reqTabPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        reqTabPanel.add(reqScroll, BorderLayout.CENTER);

        // Tab 2: User Accounts Table
        String[] userCols = {"User ID", "Username", "Role", "Full Name", "City", "Blood Group", "Phone"};
        userModel = new DefaultTableModel(userCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new ModernTable(userModel);
        JScrollPane userScroll = ModernTable.createScrollPane(userTable);

        JPanel userTabPanel = new JPanel(new BorderLayout(0, 8));
        userTabPanel.setOpaque(true);
        userTabPanel.setBackground(Color.WHITE);
        userTabPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        userTabPanel.add(userScroll, BorderLayout.CENTER);

        tabbedPane.addTab("Global Blood Requests", reqTabPanel);
        tabbedPane.addTab("User Registry", userTabPanel);

        mainContent.add(tabbedPane);

        add(mainContent, BorderLayout.CENTER);
    }

    public void refreshAllData() {
        headerPanel.refreshState();

        // Fetch blood requests
        NetworkManager.getInstance().getBloodRequests("", "", "")
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    requestModel.setRowCount(0);
                    requestList.clear();

                    if (response.isSuccess()) {
                        requestList.addAll(ResponseHandler.parseBloodRequests(response));
                        int openCount = 0;
                        for (BloodRequest br : requestList) {
                            if ("OPEN".equalsIgnoreCase(br.getStatus())) openCount++;
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
                        requestsCard.setValue(Integer.toString(openCount));
                        requestsCard.setSubtitle(requestList.size() + " total requests");
                    }
                }))
                .exceptionally(throwable -> null);

        // Populate users table from mock or local session data
        List<User> users;
        if (SessionManager.getInstance().isMockMode()) {
            users = MockDataService.getInstance().getAllMockUsers();
        } else {
            users = new ArrayList<>();
            User me = SessionManager.getInstance().getCurrentUser();
            if (me != null) users.add(me);
        }

        userModel.setRowCount(0);
        int donors = 0;
        int recipients = 0;

        for (User u : users) {
            if ("DONOR".equalsIgnoreCase(u.getRole())) donors++;
            if ("RECIPIENT".equalsIgnoreCase(u.getRole())) recipients++;

            userModel.addRow(new Object[]{
                    u.getUserId(),
                    u.getUsername(),
                    u.getRole(),
                    u.getFullName(),
                    u.getCity(),
                    ValidationUtils.toDisplayBloodGroup(u.getBloodGroup()),
                    u.getPhone()
            });
        }

        usersCard.setValue(Integer.toString(users.size()));
        donorsCard.setValue(Integer.toString(donors));
        recipientsCard.setValue(Integer.toString(recipients));
    }
}
