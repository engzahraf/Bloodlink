package com.bloodlink.client.screens;

import com.bloodlink.client.models.Donor;
import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.network.ResponseHandler;
import com.bloodlink.client.ui.NavigationManager;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.ui.components.ModernButton;
import com.bloodlink.client.ui.components.ModernTable;
import com.bloodlink.client.ui.components.ModernTextField;
import com.bloodlink.client.utils.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Donor Search and Matching Panel.
 * Allows users to search for compatible donors by blood group and location,
 * and initiate direct communications.
 */
public class DonorSearchPanel extends JPanel {

    private final NavigationManager navigationManager;
    private final JDialog parentDialog;

    private JComboBox<String> bloodGroupFilter;
    private ModernTextField cityFilter;
    private JCheckBox availableOnlyCheck;
    private ModernButton searchButton;
    private ModernButton messageButton;
    private ModernTable table;
    private DefaultTableModel tableModel;
    private JLabel countLabel;

    private final List<Donor> currentResults = new ArrayList<>();

    public DonorSearchPanel(NavigationManager navigationManager, JDialog parentDialog) {
        this.navigationManager = navigationManager;
        this.parentDialog = parentDialog;
        initUI();
        performSearch();
    }

    private void initUI() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setOpaque(false);
        JLabel title = new JLabel("Search Compatible Blood Donors");
        title.setFont(ThemeManager.FONT_HEADER);
        title.setForeground(ThemeManager.COLOR_PRIMARY);

        JLabel sub = new JLabel("Filter by blood compatibility and geographical proximity");
        sub.setFont(ThemeManager.FONT_TAGLINE);
        sub.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        headerPanel.add(title);
        headerPanel.add(sub);

        // Filter Bar
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 8));
        filterCard.setOpaque(true);
        filterCard.setBackground(new Color(0xF8, 0xF9, 0xFA));
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel bgLbl = new JLabel("Blood Group:");
        bgLbl.setFont(ThemeManager.FONT_BODY_BOLD);
        bloodGroupFilter = new JComboBox<>(new String[]{"All Groups", "O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-"});
        bloodGroupFilter.setFont(ThemeManager.FONT_BODY);

        JLabel cityLbl = new JLabel("City:");
        cityLbl.setFont(ThemeManager.FONT_BODY_BOLD);
        cityFilter = new ModernTextField("", "e.g. Colombo", 10);

        availableOnlyCheck = new JCheckBox("Available Only");
        availableOnlyCheck.setFont(ThemeManager.FONT_BODY);
        availableOnlyCheck.setSelected(true);
        availableOnlyCheck.setOpaque(false);

        searchButton = new ModernButton("Search Donors", ModernButton.ButtonStyle.PRIMARY);
        searchButton.setFont(ThemeManager.FONT_CAPTION);
        searchButton.addActionListener(e -> performSearch());

        ModernButton clearBtn = new ModernButton("Reset", ModernButton.ButtonStyle.SECONDARY);
        clearBtn.setFont(ThemeManager.FONT_CAPTION);
        clearBtn.addActionListener(e -> {
            bloodGroupFilter.setSelectedIndex(0);
            cityFilter.setText("");
            availableOnlyCheck.setSelected(true);
            performSearch();
        });

        filterCard.add(bgLbl);
        filterCard.add(bloodGroupFilter);
        filterCard.add(cityLbl);
        filterCard.add(cityFilter);
        filterCard.add(availableOnlyCheck);
        filterCard.add(searchButton);
        filterCard.add(clearBtn);

        // Center Table
        String[] columns = {"Donor Name", "Blood Group", "City / Location", "Status", "Donations", "Contact Phone"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new ModernTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            boolean hasSelection = table.getSelectedRow() >= 0;
            messageButton.setEnabled(hasSelection);
        });

        JScrollPane scrollPane = ModernTable.createScrollPane(table);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(12, 0, 12, 0));
        centerPanel.add(filterCard, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Footer Bar
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        countLabel = new JLabel("0 donors matching criteria");
        countLabel.setFont(ThemeManager.FONT_CAPTION);
        countLabel.setForeground(ThemeManager.COLOR_TEXT_SECONDARY);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setOpaque(false);

        ModernButton closeBtn = new ModernButton("Close", ModernButton.ButtonStyle.SECONDARY);
        closeBtn.addActionListener(e -> {
            if (parentDialog != null) {
                parentDialog.dispose();
            }
        });

        messageButton = new ModernButton("💬 Message Selected Donor", ModernButton.ButtonStyle.PRIMARY);
        messageButton.setEnabled(false);
        messageButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0 && selectedRow < currentResults.size()) {
                Donor selectedDonor = currentResults.get(selectedRow);
                JFrame parentFrame = (parentDialog != null && parentDialog.getOwner() instanceof JFrame) ?
                        (JFrame) parentDialog.getOwner() : navigationManager.getActiveFrame();
                navigationManager.showMessagingDialog(parentFrame, selectedDonor.getUserId(), selectedDonor.getFullName());
            }
        });

        actionButtons.add(closeBtn);
        actionButtons.add(messageButton);

        footer.add(countLabel, BorderLayout.WEST);
        footer.add(actionButtons, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    private void performSearch() {
        String bgSelection = (String) bloodGroupFilter.getSelectedItem();
        String wireBg = (bgSelection != null && !bgSelection.startsWith("All")) ?
                ValidationUtils.toCanonicalWireBloodGroup(bgSelection) : "";
        String city = cityFilter.getText().trim();
        boolean availableOnly = availableOnlyCheck.isSelected();

        searchButton.setEnabled(false);
        searchButton.setText("Searching...");

        NetworkManager.getInstance().matchDonors(wireBg, city)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    searchButton.setEnabled(true);
                    searchButton.setText("Search Donors");
                    tableModel.setRowCount(0);
                    currentResults.clear();

                    if (response.isSuccess()) {
                        List<Donor> list = ResponseHandler.parseDonors(response);
                        for (Donor d : list) {
                            if (availableOnly && !d.isAvailable()) {
                                continue;
                            }
                            currentResults.add(d);
                            tableModel.addRow(new Object[]{
                                    d.getFullName(),
                                    ValidationUtils.toDisplayBloodGroup(d.getBloodGroup()),
                                    d.getCity(),
                                    d.isAvailable() ? "AVAILABLE" : "UNAVAILABLE",
                                    d.getTotalDonations() + " donations",
                                    d.getPhone()
                            });
                        }
                        countLabel.setText(currentResults.size() + " donors matching criteria");
                    } else {
                        countLabel.setText("No donors found: " + response.getError());
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        searchButton.setEnabled(true);
                        searchButton.setText("Search Donors");
                        countLabel.setText("Search failed: " + throwable.getMessage());
                    });
                    return null;
                });
    }
}
