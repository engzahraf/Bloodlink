package com.bloodlink.server;

import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolAction;
import com.bloodlink.protocol.ProtocolPayload;
import com.bloodlink.protocol.ResponseStatus;
import com.bloodlink.services.IAuthService;
import com.bloodlink.services.IBloodMatchingService;
import com.bloodlink.services.IMessagingService;
import com.bloodlink.services.INotificationService;
import com.bloodlink.util.ServerLogger;

/**
 * Dispatches client request packets to the appropriate service handlers.
 */
public class RequestDispatcher {

    private final IAuthService authService;
    private final IBloodMatchingService bloodMatchingService;
    private final INotificationService notificationService;
    private final IMessagingService messagingService;

    public RequestDispatcher() {
        this(null, null, null, null);
    }

    public RequestDispatcher(IAuthService authService,
                             IBloodMatchingService bloodMatchingService,
                             INotificationService notificationService,
                             IMessagingService messagingService) {
        this.authService = authService;
        this.bloodMatchingService = bloodMatchingService;
        this.notificationService = notificationService;
        this.messagingService = messagingService;
    }

    /**
     * Processes an incoming packet from a client and returns a response packet.
     *
     * @param client the client connection that submitted the request
     * @param request the parsed incoming packet
     * @return response packet to return to client
     */
    public Packet dispatch(ClientHandler client, Packet request) {
        if (request == null) {
            return Packet.createErrorResponse("UNKNOWN", "", ResponseStatus.BAD_REQUEST, "Null request packet");
        }

        String action = request.getAction();
        String requestId = request.getRequestId();
        ProtocolPayload payload = request.getPayload();

        ServerLogger.debug("Dispatching action [%s] with requestId [%s] from session [%s]",
                action, requestId, client.getSessionId());

        // Heartbeat / ping
        if (ProtocolAction.PING.equalsIgnoreCase(action)) {
            return Packet.createPong(requestId);
        }

        // Authentication & Session
        if (ProtocolAction.LOGIN.equalsIgnoreCase(action)) {
            return (authService != null)
                    ? authService.handleLogin(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }
        if (ProtocolAction.REGISTER.equalsIgnoreCase(action)) {
            return (authService != null)
                    ? authService.handleRegister(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }
        if (ProtocolAction.LOGOUT.equalsIgnoreCase(action)) {
            return (authService != null)
                    ? authService.handleLogout(client, requestId)
                    : serviceUnavailableResponse(action, requestId);
        }
        if (ProtocolAction.GET_PROFILE.equalsIgnoreCase(action)) {
            return (authService != null)
                    ? authService.handleGetProfile(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }

        // Blood Requests & Matching
        if (ProtocolAction.CREATE_BLOOD_REQUEST.equalsIgnoreCase(action)) {
            return (bloodMatchingService != null)
                    ? bloodMatchingService.handleCreateBloodRequest(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }
        if (ProtocolAction.MATCH_DONORS.equalsIgnoreCase(action)) {
            return (bloodMatchingService != null)
                    ? bloodMatchingService.handleMatchDonors(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }
        if (ProtocolAction.GET_BLOOD_REQUESTS.equalsIgnoreCase(action)) {
            return (bloodMatchingService != null)
                    ? bloodMatchingService.handleGetBloodRequests(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }
        if (ProtocolAction.UPDATE_BLOOD_REQUEST_STATUS.equalsIgnoreCase(action)) {
            return (bloodMatchingService != null)
                    ? bloodMatchingService.handleUpdateBloodRequestStatus(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }
        if (ProtocolAction.UPDATE_DONOR_AVAILABILITY.equalsIgnoreCase(action)) {
            return (bloodMatchingService != null)
                    ? bloodMatchingService.handleUpdateDonorAvailability(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }

        // Notifications
        if (ProtocolAction.GET_NOTIFICATIONS.equalsIgnoreCase(action)) {
            return (notificationService != null)
                    ? notificationService.handleGetNotifications(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }

        // Donor-Recipient Messaging
        if (ProtocolAction.SEND_MESSAGE.equalsIgnoreCase(action)) {
            return (messagingService != null)
                    ? messagingService.handleSendMessage(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }
        if (ProtocolAction.GET_MESSAGES.equalsIgnoreCase(action)) {
            return (messagingService != null)
                    ? messagingService.handleGetMessages(client, requestId, payload)
                    : serviceUnavailableResponse(action, requestId);
        }

        return Packet.createErrorResponse(action, requestId, ResponseStatus.NOT_FOUND,
                "Unknown or unsupported action: " + action);
    }

    private Packet serviceUnavailableResponse(String action, String requestId) {
        return Packet.createErrorResponse(action, requestId, ResponseStatus.INTERNAL_ERROR,
                "Service handler for " + action + " is not configured");
    }
}
