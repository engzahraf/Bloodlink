package com.bloodlink.client.screens;

import com.bloodlink.client.models.Message;
import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.network.ResponseHandler;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.ui.components.ModernButton;
import com.bloodlink.client.ui.components.ModernTextField;
import com.bloodlink.client.utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Direct donor-recipient messaging chat interface.
 * Supports sending messages over TCP sockets and receiving real-time incoming push messages.
 */
public class MessagingPanel extends JPanel implements NetworkManager.PushEventListener {

    private final String targetUserId;
    private final String targetUserName;

    private JPanel chatHistoryPanel;
    private JScrollPane scrollPane;
    private ModernTextField messageInput;
    private ModernButton sendButton;

    private final List<Message> messageList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public MessagingPanel(String targetUserId, String targetUserName) {
        this.targetUserId = targetUserId != null ? targetUserId : "usr_general";
        this.targetUserName = targetUserName != null ? targetUserName : "BloodLink User";
        initUI();
        NetworkManager.getInstance().addPushEventListener(this);
        loadChatHistory();
    }

    private void initUI() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Contact Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(true);
        headerPanel.setBackground(new Color(0xF8, 0xF9, 0xFA));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JPanel contactInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        contactInfo.setOpaque(false);
        JLabel nameLabel = new JLabel("💬 " + targetUserName);
        nameLabel.setFont(ThemeManager.FONT_TITLE);
        nameLabel.setForeground(ThemeManager.COLOR_TEXT_PRIMARY);

        JLabel idLabel = new JLabel("ID: " + targetUserId + " • Direct TCP Communication Channel");
        idLabel.setFont(ThemeManager.FONT_CAPTION);
        idLabel.setForeground(ThemeManager.COLOR_TEXT_MUTED);

        contactInfo.add(nameLabel);
        contactInfo.add(idLabel);

        ModernButton refreshBtn = new ModernButton("Refresh", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setFont(ThemeManager.FONT_CAPTION);
        refreshBtn.addActionListener(e -> loadChatHistory());

        headerPanel.add(contactInfo, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);

        // Center Message History
        chatHistoryPanel = new JPanel();
        chatHistoryPanel.setOpaque(true);
        chatHistoryPanel.setBackground(new Color(0xFA, 0xFB, 0xFC));
        chatHistoryPanel.setLayout(new BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS));
        chatHistoryPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        scrollPane = new JScrollPane(chatHistoryPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.COLOR_BORDER, 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Bottom Input Row
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        messageInput = new ModernTextField("", "Type your message to " + targetUserName + "...", 30);
        messageInput.addActionListener(e -> handleSendMessage());

        sendButton = new ModernButton("Send", ModernButton.ButtonStyle.PRIMARY);
        sendButton.addActionListener(e -> handleSendMessage());

        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    private void loadChatHistory() {
        NetworkManager.getInstance().getMessages(targetUserId, 50)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    messageList.clear();
                    chatHistoryPanel.removeAll();

                    String myId = SessionManager.getInstance().getCurrentUserId();
                    if (response.isSuccess()) {
                        messageList.addAll(ResponseHandler.parseMessages(response, myId));
                        for (Message m : messageList) {
                            appendMessageBubble(m);
                        }
                    } else {
                        // Show placeholder empty state
                        appendSystemNotice("Start a conversation with " + targetUserName);
                    }
                    scrollToBottom();
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> appendSystemNotice("Conversation history unavailable: " + throwable.getMessage()));
                    return null;
                });
    }

    private void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        messageInput.setText("");
        sendButton.setEnabled(false);

        String myId = SessionManager.getInstance().getCurrentUserId();
        String myName = SessionManager.getInstance().getCurrentUser() != null ?
                SessionManager.getInstance().getCurrentUser().getFullName() : "Me";

        Message localMsg = new Message(
                "msg_" + System.currentTimeMillis(),
                myId,
                myName,
                targetUserId,
                targetUserName,
                content,
                System.currentTimeMillis(),
                false
        );

        appendMessageBubble(localMsg);
        scrollToBottom();

        NetworkManager.getInstance().sendMessage(targetUserId, content)
                .thenAccept(response -> SwingUtilities.invokeLater(() -> {
                    sendButton.setEnabled(true);
                    if (!response.isSuccess()) {
                        appendSystemNotice("⚠️ Failed to deliver: " + response.getError());
                    }
                }))
                .exceptionally(throwable -> {
                    SwingUtilities.invokeLater(() -> {
                        sendButton.setEnabled(true);
                        appendSystemNotice("⚠️ Network error sending message: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void appendMessageBubble(Message msg) {
        JPanel bubbleWrapper = new JPanel(new FlowLayout(msg.isIncoming() ? FlowLayout.LEFT : FlowLayout.RIGHT, 4, 4));
        bubbleWrapper.setOpaque(false);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(msg.isIncoming() ? ThemeManager.COLOR_BORDER : ThemeManager.COLOR_PRIMARY_DARK, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        if (msg.isIncoming()) {
            bubble.setBackground(Color.WHITE);
        } else {
            bubble.setBackground(ThemeManager.COLOR_PRIMARY);
        }

        JLabel senderLabel = new JLabel((msg.isIncoming() ? targetUserName : "You") + "  " + timeFormat.format(new Date(msg.getTimestamp())));
        senderLabel.setFont(ThemeManager.FONT_CAPTION);
        senderLabel.setForeground(msg.isIncoming() ? ThemeManager.COLOR_TEXT_MUTED : ThemeManager.COLOR_PRIMARY_LIGHT);

        JLabel textLabel = new JLabel("<html><body style='width: 320px;'>" + escapeHtml(msg.getContent()) + "</body></html>");
        textLabel.setFont(ThemeManager.FONT_BODY);
        textLabel.setForeground(msg.isIncoming() ? ThemeManager.COLOR_TEXT_PRIMARY : Color.WHITE);

        bubble.add(senderLabel);
        bubble.add(Box.createVerticalStrut(2));
        bubble.add(textLabel);

        bubbleWrapper.add(bubble);
        chatHistoryPanel.add(bubbleWrapper);
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
    }

    private void appendSystemNotice(String notice) {
        JPanel noticePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
        noticePanel.setOpaque(false);
        JLabel lbl = new JLabel(notice);
        lbl.setFont(ThemeManager.FONT_CAPTION);
        lbl.setForeground(ThemeManager.COLOR_TEXT_MUTED);
        noticePanel.add(lbl);
        chatHistoryPanel.add(noticePanel);
        chatHistoryPanel.revalidate();
        chatHistoryPanel.repaint();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = scrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>");
    }

    // Push Event Listener Callbacks
    @Override
    public void onIncomingMessageReceived(Message message) {
        if (message != null && (targetUserId.equals(message.getSenderId()) || targetUserId.equals(message.getRecipientId()))) {
            appendMessageBubble(message);
            scrollToBottom();
        }
    }

    @Override
    public void onUrgentAlertReceived(String bg, String hosp, String city, String urg, String title) {
    }

    @Override
    public void onConnectionStateChanged(boolean connected, String message) {
    }
}
