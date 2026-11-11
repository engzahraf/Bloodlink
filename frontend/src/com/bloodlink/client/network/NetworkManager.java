package com.bloodlink.client.network;

import com.bloodlink.client.models.*;
import com.bloodlink.client.network.ResponseHandler.ParsedResponse;
import com.bloodlink.client.utils.SessionManager;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-level asynchronous network manager.
 * Manages request-response correlation via request IDs, routes server push events,
 * and ensures all UI notifications are delivered safely on Swing's Event Dispatch Thread.
 */
public class NetworkManager implements SocketClient.SocketListener {

    private static final NetworkManager INSTANCE = new NetworkManager();

    public interface PushEventListener {
        void onUrgentAlertReceived(String bloodGroup, String hospital, String city, String urgency, String title);
        void onIncomingMessageReceived(Message message);
        void onConnectionStateChanged(boolean connected, String message);
    }

    private SocketClient socketClient;
    private final AtomicLong requestIdCounter = new AtomicLong(100);
    private final ConcurrentMap<String, CompletableFuture<ParsedResponse>> pendingRequests = new ConcurrentHashMap<>();
    private final List<PushEventListener> pushListeners = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BloodLink-TimeoutScheduler");
        t.setDaemon(true);
        return t;
    });

    private NetworkManager() {
    }

    public static NetworkManager getInstance() {
        return INSTANCE;
    }

    public synchronized void connect(String host, int port) throws IOException {
        disconnect();
        socketClient = new SocketClient(host, port, this);
        socketClient.connect();
    }

    public synchronized void disconnect() {
        if (socketClient != null) {
            socketClient.disconnect();
            socketClient = null;
        }

        // Cancel pending requests
        for (Map.Entry<String, CompletableFuture<ParsedResponse>> entry : pendingRequests.entrySet()) {
            entry.getValue().completeExceptionally(new IOException("Disconnected from server"));
        }
        pendingRequests.clear();
    }

    public boolean isConnected() {
        return socketClient != null && socketClient.isConnected();
    }

    public String nextRequestId() {
        return "req_" + requestIdCounter.incrementAndGet();
    }

    public void addPushEventListener(PushEventListener listener) {
        if (listener != null && !pushListeners.contains(listener)) {
            pushListeners.add(listener);
        }
    }

    public void removePushEventListener(PushEventListener listener) {
        pushListeners.remove(listener);
    }

    /**
     * Sends a request packet and returns a CompletableFuture correlated to the request ID.
     */
    public CompletableFuture<ParsedResponse> sendRequest(String wireString, String requestId) {
        CompletableFuture<ParsedResponse> future = new CompletableFuture<>();

        // If mock mode is enabled, immediately delegate to MockDataService
        if (SessionManager.getInstance().isMockMode()) {
            return MockDataService.getInstance().handleMockRequest(wireString, requestId);
        }

        if (!isConnected()) {
            future.completeExceptionally(new IOException("Cannot send request: Not connected to TCP server."));
            return future;
        }

        pendingRequests.put(requestId, future);

        // Schedule timeout after 8 seconds
        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
            CompletableFuture<ParsedResponse> pending = pendingRequests.remove(requestId);
            if (pending != null && !pending.isDone()) {
                pending.completeExceptionally(new TimeoutException("Server did not respond within 8000ms for " + requestId));
            }
        }, 8000, TimeUnit.MILLISECONDS);

        future.whenComplete((res, err) -> timeoutTask.cancel(true));

        try {
            socketClient.send(wireString);
        } catch (IOException e) {
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
        }

        return future;
    }

    // ==================== High-Level Business Methods ====================

    public CompletableFuture<ParsedResponse> login(String username, String password) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildLoginRequest(reqId, username, password);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> register(String username, String password, String role,
                                                      String fullName, String bloodGroup, String city, String phone, String email) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildRegisterRequest(reqId, username, password, role, fullName, bloodGroup, city, phone, email);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> getProfile(String userId) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildGetProfileRequest(reqId, userId);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> createBloodRequest(String bloodGroup, int units, String hospital, String city, String urgency, String notes) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildCreateBloodRequest(reqId, bloodGroup, units, hospital, city, urgency, notes);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> getBloodRequests(String status, String bloodGroup, String city) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildGetBloodRequests(reqId, status, bloodGroup, city);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> updateBloodRequestStatus(String bloodRequestId, String newStatus) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildUpdateBloodRequestStatus(reqId, bloodRequestId, newStatus);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> matchDonors(String bloodGroup, String city) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildMatchDonors(reqId, bloodGroup, city);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> updateDonorAvailability(String userId, boolean available) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildUpdateDonorAvailability(reqId, userId, available);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> sendMessage(String recipientId, String content) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildSendMessage(reqId, recipientId, content);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> getMessages(String contactUserId, int limit) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildGetMessages(reqId, contactUserId, limit);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> getNotifications(int limit) {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildGetNotifications(reqId, limit);
        return sendRequest(packet, reqId);
    }

    public CompletableFuture<ParsedResponse> ping() {
        String reqId = nextRequestId();
        String packet = RequestBuilder.buildPing(reqId);
        return sendRequest(packet, reqId);
    }

    // ==================== SocketClient Listener Callbacks ====================

    @Override
    public void onPacketReceived(String rawWireString) {
        ParsedResponse response = ResponseHandler.parseWirePacket(rawWireString);
        String type = response.getType();
        String action = response.getAction();
        String requestId = response.getRequestId();

        // 1. Check for synchronous response matching pending request ID
        if ("RES".equals(type) && !requestId.isEmpty()) {
            CompletableFuture<ParsedResponse> pending = pendingRequests.remove(requestId);
            if (pending != null) {
                pending.complete(response);
                return;
            }
        }

        // 2. Check for asynchronous Server Push Events (EVENT / MSG)
        if ("EVENT".equals(type) && "URGENT_ALERT".equals(action)) {
            String bloodGroup = response.getOrDefault("bloodGroup", "Unknown");
            String hospital = response.getOrDefault("hospital", "General Hospital");
            String city = response.getOrDefault("city", "");
            String urgency = response.getOrDefault("urgency", "URGENT");
            String title = response.getOrDefault("title", "URGENT BLOOD NEEDED");

            SwingUtilities.invokeLater(() -> {
                for (PushEventListener l : pushListeners) {
                    try {
                        l.onUrgentAlertReceived(bloodGroup, hospital, city, urgency, title);
                    } catch (Exception ignored) {
                    }
                }
            });
        } else if ("MSG".equals(type) || "SEND_MESSAGE".equals(action) || "RECEIVE_MESSAGE".equals(action)) {
            String msgId = response.getOrDefault("messageId", "msg_" + System.currentTimeMillis());
            String senderId = response.getOrDefault("senderId", "");
            String recipientId = response.getOrDefault("recipientId", "");
            String content = response.getOrDefault("content", "");
            long ts = response.getLong("timestamp", System.currentTimeMillis());

            String myId = SessionManager.getInstance().getCurrentUserId();
            boolean isIncoming = myId == null || !myId.equals(senderId);
            Message msg = new Message(msgId, senderId, senderId, recipientId, recipientId, content, ts, isIncoming);

            SwingUtilities.invokeLater(() -> {
                for (PushEventListener l : pushListeners) {
                    try {
                        l.onIncomingMessageReceived(msg);
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    @Override
    public void onConnected() {
        SwingUtilities.invokeLater(() -> {
            for (PushEventListener l : pushListeners) {
                try {
                    l.onConnectionStateChanged(true, "Connected to server at " + socketClient.getHost() + ":" + socketClient.getPort());
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            for (PushEventListener l : pushListeners) {
                try {
                    l.onConnectionStateChanged(false, reason);
                } catch (Exception ignored) {
                }
            }
        });
    }

    @Override
    public void onError(Throwable t) {
        SwingUtilities.invokeLater(() -> {
            for (PushEventListener l : pushListeners) {
                try {
                    l.onConnectionStateChanged(false, "Socket Error: " + t.getMessage());
                } catch (Exception ignored) {
                }
            }
        });
    }
}
