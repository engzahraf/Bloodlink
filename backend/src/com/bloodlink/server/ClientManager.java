package com.bloodlink.server;

import com.bloodlink.protocol.Packet;
import com.bloodlink.util.ServerLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for tracking, querying, and routing messages to active client connections.
 */
public class ClientManager {

    // Key: sessionId -> ClientHandler
    private final Map<String, ClientHandler> activeClientsBySession = new ConcurrentHashMap<>();

    // Key: userId -> Set of active sessionIds
    private final Map<String, Set<String>> userSessionIndex = new ConcurrentHashMap<>();

    public ClientManager() {
    }

    /**
     * Registers a new connected client handler.
     */
    public void registerClient(ClientHandler handler) {
        if (handler == null) {
            return;
        }
        activeClientsBySession.put(handler.getSessionId(), handler);
        ServerLogger.info("Client registered [Session: %s, Remote: %s]. Active connections: %d",
                handler.getSessionId(), handler.getRemoteAddress(), activeClientsBySession.size());
    }

    /**
     * Unregisters a disconnected client handler and cleans up user session bindings.
     */
    public void unregisterClient(ClientHandler handler) {
        if (handler == null) {
            return;
        }
        String sessionId = handler.getSessionId();
        activeClientsBySession.remove(sessionId);

        String userId = handler.getAuthenticatedUserId();
        if (userId != null) {
            Set<String> sessions = userSessionIndex.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessionIndex.remove(userId);
                }
            }
        }

        ServerLogger.info("Client unregistered [Session: %s, User: %s]. Remaining active: %d",
                sessionId, (userId != null ? userId : "Anonymous"), activeClientsBySession.size());
    }

    /**
     * Associates an authenticated user ID with a client's active session safely.
     * Cleans up any prior user binding on this session if re-authenticating.
     */
    public void bindUser(String userId, ClientHandler handler) {
        if (userId == null || handler == null) {
            return;
        }

        // Clean up previous user association on this connection if switching users
        String previousUserId = handler.getAuthenticatedUserId();
        if (previousUserId != null && !previousUserId.equals(userId)) {
            unbindUser(previousUserId, handler);
        }

        userSessionIndex.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(handler.getSessionId());
        handler.setAuthenticatedUser(userId);
        ServerLogger.info("User bound to session [User: %s, Session: %s]", userId, handler.getSessionId());
    }

    /**
     * Unbinds an authenticated user from a session (e.g. on logout).
     */
    public void unbindUser(String userId, ClientHandler handler) {
        if (userId == null || handler == null) {
            return;
        }
        Set<String> sessions = userSessionIndex.get(userId);
        if (sessions != null) {
            sessions.remove(handler.getSessionId());
            if (sessions.isEmpty()) {
                userSessionIndex.remove(userId);
            }
        }
        handler.clearAuthenticatedUser();
        ServerLogger.info("User unbound from session [User: %s, Session: %s]", userId, handler.getSessionId());
    }

    /**
     * Checks if a user currently has at least one active connection.
     */
    public boolean isUserOnline(String userId) {
        if (userId == null) {
            return false;
        }
        Set<String> sessions = userSessionIndex.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Sends a packet directly to an authenticated user across all their active sessions.
     *
     * @return true if the packet was successfully delivered to at least one active session
     */
    public boolean sendToUser(String userId, Packet packet) {
        if (userId == null || packet == null) {
            return false;
        }
        Set<String> sessions = userSessionIndex.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }

        boolean delivered = false;
        for (String sessionId : sessions) {
            ClientHandler handler = activeClientsBySession.get(sessionId);
            if (handler != null && handler.isRunning()) {
                try {
                    handler.sendPacket(packet);
                    delivered = true;
                } catch (IOException e) {
                    ServerLogger.warn("Failed sending packet to user %s on session %s: %s", userId, sessionId, e.getMessage());
                }
            }
        }
        return delivered;
    }

    /**
     * Broadcasts a packet to all connected clients.
     */
    public void broadcast(Packet packet) {
        if (packet == null) {
            return;
        }
        for (ClientHandler handler : activeClientsBySession.values()) {
            if (handler.isRunning()) {
                try {
                    handler.sendPacket(packet);
                } catch (IOException e) {
                    ServerLogger.warn("Broadcast error to session %s: %s", handler.getSessionId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Broadcasts a packet to all authenticated clients with a specific role (e.g., DONOR, HOSPITAL).
     */
    public int broadcastToRole(String role, Packet packet) {
        if (role == null || packet == null) {
            return 0;
        }
        int count = 0;
        for (ClientHandler handler : activeClientsBySession.values()) {
            if (handler.isRunning() && role.equalsIgnoreCase(handler.getUserRole())) {
                try {
                    handler.sendPacket(packet);
                    count++;
                } catch (IOException e) {
                    ServerLogger.warn("Role broadcast error to session %s: %s", handler.getSessionId(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Broadcasts a notification packet to a specific set of user IDs (e.g., matched eligible donors).
     */
    public int broadcastToUsers(Collection<String> userIds, Packet packet) {
        if (userIds == null || packet == null) {
            return 0;
        }
        int deliveredCount = 0;
        for (String userId : userIds) {
            if (sendToUser(userId, packet)) {
                deliveredCount++;
            }
        }
        return deliveredCount;
    }

    public ClientHandler getClientBySessionId(String sessionId) {
        return activeClientsBySession.get(sessionId);
    }

    public int getActiveConnectionCount() {
        return activeClientsBySession.size();
    }

    public List<ClientHandler> getAllActiveClients() {
        return Collections.unmodifiableList(new ArrayList<>(activeClientsBySession.values()));
    }

    /**
     * Disconnects and shuts down all active client connections safely.
     */
    public void disconnectAll(String reason) {
        ServerLogger.info("Disconnecting all active clients. Reason: %s", reason);
        for (ClientHandler handler : activeClientsBySession.values()) {
            handler.close();
        }
        activeClientsBySession.clear();
        userSessionIndex.clear();
    }
}
