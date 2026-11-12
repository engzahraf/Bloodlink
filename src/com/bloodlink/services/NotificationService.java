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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements real-time push notification routing and event delivery to active clients.
 */
public class NotificationService implements INotificationService {

    private static final AtomicLong EVENT_COUNTER = new AtomicLong(1);

    private final ClientManager clientManager;
    private final IDatabaseService databaseService;

    public NotificationService(ClientManager clientManager, IDatabaseService databaseService) {
        this.clientManager = clientManager;
        this.databaseService = databaseService;
    }

    @Override
    public boolean sendNotification(String targetUserId, String title, String message, String urgency) {
        if (targetUserId == null || title == null) {
            return false;
        }

        long timestamp = System.currentTimeMillis();
        String safeUrgency = (urgency != null) ? urgency.toUpperCase() : "NORMAL";

        // 1. Persist notification in database for offline history
        if (databaseService != null) {
            try {
                databaseService.saveNotification(targetUserId, title, message != null ? message : "", safeUrgency, timestamp);
            } catch (Exception e) {
                ServerLogger.warn("Failed to persist notification for %s: %s", targetUserId, e.getMessage());
            }
        }

        // 2. Deliver in real-time if user is currently connected
        String eventId = "evt-" + EVENT_COUNTER.getAndIncrement();
        ProtocolPayload payload = new ProtocolPayload()
                .set("title", title)
                .set("message", message != null ? message : "")
                .set("urgency", safeUrgency)
                .set("timestamp", timestamp);

        Packet eventPacket = Packet.createEvent(ProtocolAction.NOTIFICATION, eventId, payload);
        boolean delivered = clientManager.sendToUser(targetUserId, eventPacket);

        if (delivered) {
            ServerLogger.info("Real-time notification [%s] delivered to user %s", title, targetUserId);
        } else {
            ServerLogger.info("User %s offline; notification [%s] queued in database", targetUserId, title);
        }

        return delivered;
    }

    @Override
    public int notifyEligibleDonors(Collection<String> donorUserIds, Map<String, String> requestDetails) {
        if (donorUserIds == null || donorUserIds.isEmpty()) {
            return 0;
        }

        String eventId = "alert-" + EVENT_COUNTER.getAndIncrement();
        String bloodGroup = requestDetails.getOrDefault("bloodGroup", "Unknown");
        String hospital = requestDetails.getOrDefault("hospital", "Local Hospital");
        String city = requestDetails.getOrDefault("city", "");
        String urgency = requestDetails.getOrDefault("urgency", "URGENT");
        String requestId = requestDetails.getOrDefault("requestId", "");

        ProtocolPayload payload = new ProtocolPayload()
                .set("action", ProtocolAction.URGENT_ALERT)
                .set("requestId", requestId)
                .set("bloodGroup", bloodGroup)
                .set("hospital", hospital)
                .set("city", city)
                .set("urgency", urgency)
                .set("title", "URGENT: Blood Donation Needed for " + bloodGroup)
                .set("message", String.format("Urgent requirement for blood group %s at %s (%s). Please respond if available.",
                        bloodGroup, hospital, city))
                .set("timestamp", System.currentTimeMillis());

        Packet alertPacket = Packet.createEvent(ProtocolAction.URGENT_ALERT, eventId, payload);

        int deliveredCount = 0;
        for (String donorId : donorUserIds) {
            // Persist notification for donor
            if (databaseService != null) {
                databaseService.saveNotification(donorId, payload.get("title"), payload.get("message"), urgency, System.currentTimeMillis());
            }

            // Real-time dispatch if online
            if (clientManager.sendToUser(donorId, alertPacket)) {
                deliveredCount++;
            }
        }

        ServerLogger.info("Urgent alert dispatched for blood request %s (%s): %d/%d donors notified online",
                requestId, bloodGroup, deliveredCount, donorUserIds.size());

        return deliveredCount;
    }

    @Override
    public int broadcastToRole(String role, String title, String message, String urgency) {
        if (role == null) {
            return 0;
        }

        String eventId = "role-evt-" + EVENT_COUNTER.getAndIncrement();
        ProtocolPayload payload = new ProtocolPayload()
                .set("title", title)
                .set("message", message != null ? message : "")
                .set("urgency", urgency != null ? urgency.toUpperCase() : "NORMAL")
                .set("timestamp", System.currentTimeMillis());

        Packet eventPacket = Packet.createEvent(ProtocolAction.NOTIFICATION, eventId, payload);
        return clientManager.broadcastToRole(role, eventPacket);
    }

    @Override
    public Packet handleGetNotifications(ClientHandler client, String requestId, ProtocolPayload payload) {
        if (!client.isAuthenticated()) {
            return Packet.createErrorResponse(ProtocolAction.GET_NOTIFICATIONS, requestId,
                    ResponseStatus.UNAUTHORIZED, "Authentication required to view notifications");
        }

        if (databaseService == null) {
            return Packet.createErrorResponse(ProtocolAction.GET_NOTIFICATIONS, requestId,
                    ResponseStatus.INTERNAL_ERROR, "Database service unavailable");
        }

        String userId = client.getAuthenticatedUserId();
        int limit = payload.getInt("limit", 20);
        if (limit <= 0) {
            limit = 20;
        }

        List<Map<String, String>> notifs = databaseService.getNotificationsForUser(userId, limit);
        List<String> records = new ArrayList<>();
        for (Map<String, String> n : notifs) {
            ProtocolPayload rec = new ProtocolPayload(n);
            records.add(rec.serialize());
        }

        ProtocolPayload resPayload = new ProtocolPayload()
                .set("count", records.size())
                .setList("notifications", records)
                .setList("notifications_list", records);

        return Packet.createSuccessResponse(ProtocolAction.GET_NOTIFICATIONS, requestId, resPayload);
    }
}
