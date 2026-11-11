package com.bloodlink.client.ui.components;

import com.bloodlink.client.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import java.awt.*;

/**
 * Modern styled Swing table with zebra striping, padded headers, and status pill rendering.
 */
public class ModernTable extends JTable {

    public ModernTable(TableModel model) {
        super(model);
        initModernTable();
    }

    private void initModernTable() {
        setRowHeight(38);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setGridColor(new Color(0xEE, 0xF0, 0xF2));
        setFont(ThemeManager.FONT_BODY);
        setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        setSelectionBackground(new Color(0xFF, 0xEB, 0xEE));
        setSelectionForeground(ThemeManager.COLOR_PRIMARY_DARK);
        setFillsViewportHeight(true);

        // Header Styling
        JTableHeader header = getTableHeader();
        header.setFont(ThemeManager.FONT_BODY_BOLD);
        header.setBackground(new Color(0xF4, 0xF6, 0xF8));
        header.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        header.setReorderingAllowed(false);
        header.setBorder(new LineBorder(ThemeManager.COLOR_BORDER, 1));

        // Default cell renderer with padding and zebra rows
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xFA, 0xFB, 0xFC));
                    c.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);
                }
                return c;
            }
        });
    }

    public static JScrollPane createScrollPane(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(ThemeManager.COLOR_BORDER, 1, true));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBackground(Color.WHITE);
        return scrollPane;
    }
}
