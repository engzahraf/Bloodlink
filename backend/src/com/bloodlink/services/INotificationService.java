package com.bloodlink.services;

import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.server.ClientHandler;

import java.util.Collection;
import java.util.Map;

/**
 * Service interface for real-time notification dispatch, alert routing, and notification history.
 */
public interface INotificationService {

    /**
     * Sends a real-time notification to a specific user.
     * Persists the notification via IDatabaseService and delivers it immediately if the user is online.
     */
    boolean sendNotification(String targetUserId, String title, String message, String urgency);

    /**
     * Routes an urgent blood request alert to a list of eligible donor user IDs.
     *
     * @return number of currently online donors who received real-time delivery
     */
    int notifyEligibleDonors(Collection<String> donorUserIds, Map<String, String> requestDetails);

    /**
     * Broadcasts a system alert to all users with a specific role.
     */
    int broadcastToRole(String role, String title, String message, String urgency);

    /**
     * Retrieves notification history for the authenticated client.
     */
    Packet handleGetNotifications(ClientHandler client, String requestId, ProtocolPayload payload);
}
