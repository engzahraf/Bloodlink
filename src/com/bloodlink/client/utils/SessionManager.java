package com.bloodlink.client.utils;

import com.bloodlink.client.models.User;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe singleton managing active user authentication state and session lifecycle.
 */
public final class SessionManager {

    public interface SessionListener {
        void onSessionChanged(User currentUser);
        void onSessionCleared();
    }

    private static final SessionManager INSTANCE = new SessionManager();

    private volatile User currentUser;
    private volatile boolean mockMode = false;
    private volatile String serverHost = "127.0.0.1";
    private volatile int serverPort = 9090;

    private final List<SessionListener> listeners = new CopyOnWriteArrayList<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
        for (SessionListener l : listeners) {
            try {
                l.onSessionChanged(user);
            } catch (Exception ignored) {
            }
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public String getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getUserId() : null;
    }

    public synchronized void clearSession() {
        this.currentUser = null;
        for (SessionListener l : listeners) {
            try {
                l.onSessionCleared();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        if (serverHost != null && !serverHost.trim().isEmpty()) {
            this.serverHost = serverHost.trim();
        }
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        if (serverPort > 0 && serverPort <= 65535) {
            this.serverPort = serverPort;
        }
    }

    public void addListener(SessionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SessionListener listener) {
        listeners.remove(listener);
    }
}
