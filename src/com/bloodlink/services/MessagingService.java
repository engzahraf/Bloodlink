package com.bloodlink.services;

import com.bloodlink.interfaces.IDatabaseService;
import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolAction;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.protocol.ResponseStatus;
import com.bloodlink.server.ClientHandler;
import com.bloodlink.server.ClientManager;
import com.bloodlink.util.ServerLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements donor-recipient direct chat routing and history query.
 * Enforces sender authentication and prevents impersonation.
 */
public class MessagingService implements IMessagingService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final IDatabaseService databaseService;
    private final ClientManager clientManager;

    public MessagingService(IDatabaseService databaseService, ClientManager clientManager) {
        this.databaseService = databaseService;
        this.clientManager = clientManager;
    }

    @Override
    public Packet handleSendMessage(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.SEND_MESSAGE, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to send messages");
        }

        // Impersonation prevention: senderId is strictly obtained from the authenticated session
        String senderId = client.getAuthenticatedUserId();
        String recipientId = payload.get("recipientId");
        String content = payload.get("content");

        if (recipientId == null || recipientId.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            return Packet.createErrorResponse(ProtocolAction.SEND_MESSAGE, requestId,
                    ResponseStatus.BAD_REQUEST, "Parameters 'recipientId' and 'content' are required");
        }

        String trimmedRecipient = recipientId.trim();
        String trimmedContent = content.trim();

        if (trimmedContent.length() > MAX_MESSAGE_LENGTH) {
            return Packet.createErrorResponse(ProtocolAction.SEND_MESSAGE, requestId,
                    ResponseStatus.BAD_REQUEST, "Message content exceeds maximum allowed length of " + MAX_MESSAGE_LENGTH + " characters");
        }

        if (senderId.equals(trimmedRecipient)) {
            return Packet.createErrorResponse(ProtocolAction.SEND_MESSAGE, requestId,
                    ResponseStatus.BAD_REQUEST, "Sender and recipient cannot be the same user");
        }

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.SEND_MESSAGE, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        // Verify recipient exists in the system
        Map<String, String> recipientUser = databaseService.getUserById(trimmedRecipient);
        if (recipientUser == null || recipientUser.isEmpty()) {
            return Packet.createErrorResponse(ProtocolAction.SEND_MESSAGE, requestId,
                    ResponseStatus.NOT_FOUND, "Recipient user not found: " + trimmedRecipient);
        }

        long timestamp = System.currentTimeMillis();
        String messageId = databaseService.saveMessage(senderId, trimmedRecipient, trimmedContent, timestamp);

        if (messageId == null) {
            return Packet.createErrorResponse(ProtocolAction.SEND_MESSAGE, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Failed to persist message");
        }

        // Real-time dispatch to recipient if currently connected
        ProtocolPayload msgPayload = new ProtocolPayload()
                .set("messageId", messageId)
                .set("senderId", senderId)
                .set("recipientId", trimmedRecipient)
                .set("content", trimmedContent)
                .set("timestamp", timestamp);

        Packet directMsgPacket = Packet.createMessage(messageId, msgPayload);
        boolean deliveredRealTime = clientManager.sendToUser(trimmedRecipient, directMsgPacket);

        ServerLogger.info("Chat message %s from %s to %s delivered real-time: %b",
                messageId, senderId, trimmedRecipient, deliveredRealTime);

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("messageId", messageId)
                .set("recipientId", trimmedRecipient)
                .set("deliveredRealTime", deliveredRealTime)
                .set("timestamp", timestamp);

        return Packet.createSuccessResponse(ProtocolAction.SEND_MESSAGE, requestId, resPayload);
    }

    @Override
    public Packet handleGetMessages(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.GET_MESSAGES, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to view messages");
        }

        String currentUserId = client.getAuthenticatedUserId();
        String otherUserId = payload.get("otherUserId");
        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            otherUserId = payload.get("contactUserId");
        }
        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            otherUserId = payload.get("recipientId");
        }
        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            otherUserId = payload.get("userId");
        }
        int limit = payload.getInt("limit", 50);

        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            return Packet.createErrorResponse(ProtocolAction.GET_MESSAGES, requestId,
                    ResponseStatus.BAD_REQUEST, "Parameter 'otherUserId' or 'contactUserId' is required");
        }

        String trimmedOtherUser = otherUserId.trim();

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.GET_MESSAGES, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        List<Map<String, String>> history = databaseService.getConversationHistory(currentUserId, trimmedOtherUser, limit);
        List<String> encodedMessages = new ArrayList<>();

        for (Map<String, String> msg : history) {
            ProtocolPayload rec = new ProtocolPayload(msg);
            encodedMessages.add(rec.serialize());
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("count", encodedMessages.size())
                .set("withUserId", trimmedOtherUser)
                .setList("messages", encodedMessages)
                .setList("messages_list", encodedMessages);

        return Packet.createSuccessResponse(ProtocolAction.GET_MESSAGES, requestId, resPayload);
    }
}
