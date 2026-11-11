package com.bloodlink.server;

import com.bloodlink.protocol.MalformedPacketException;
import com.bloodlink.protocol.Packet;
import com.bloodlink.protocol.ProtocolCodec;
import com.bloodlink.protocol.ResponseStatus;
import com.bloodlink.util.ServerConfig;
import com.bloodlink.util.ServerLogger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles communication for a single connected client socket.
 * Implements standard framing via DataInputStream.readUTF() and DataOutputStream.writeUTF().
 */
public class ClientHandler implements Runnable {

    private static final AtomicLong SESSION_COUNTER = new AtomicLong(1);

    private final String sessionId;
    private final Socket socket;
    private final ClientManager clientManager;
    private final RequestDispatcher requestDispatcher;

    private DataInputStream in;
    private DataOutputStream out;

    private volatile boolean running = true;
    private volatile String authenticatedUserId = null;
    private volatile String userRole = null;
    private final long connectedAt;
    private volatile long lastActivityTime;

    public ClientHandler(Socket socket, ClientManager clientManager, RequestDispatcher requestDispatcher) {
        this.sessionId = "client-" + SESSION_COUNTER.getAndIncrement();
        this.socket = socket;
        this.clientManager = clientManager;
        this.requestDispatcher = requestDispatcher;
        this.connectedAt = System.currentTimeMillis();
        this.lastActivityTime = this.connectedAt;
    }

    @Override
    public void run() {
        try {
            // Apply socket read timeout to prevent dead connection hanging
            socket.setSoTimeout(ServerConfig.SOCKET_TIMEOUT_MS);

            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            ServerLogger.info("Client session %s established from %s", sessionId, getRemoteAddress());

            while (running && !socket.isClosed()) {
                try {
                    Packet requestPacket = ProtocolCodec.readPacket(in);
                    this.lastActivityTime = System.currentTimeMillis();

                    ServerLogger.info("Received from session %s: type=%s, action=%s, reqId=%s",
                            sessionId, requestPacket.getType(), requestPacket.getAction(), requestPacket.getRequestId());

                    // Dispatch packet to request dispatcher
                    Packet responsePacket = requestDispatcher.dispatch(this, requestPacket);
                    if (responsePacket != null) {
                        sendPacket(responsePacket);
                    }

                } catch (SocketTimeoutException e) {
                    // Socket read timed out. We can log a debug note and continue listening or close if inactive
                    ServerLogger.debug("Socket read timeout on session %s (keep-alive active)", sessionId);
                } catch (MalformedPacketException e) {
                    ServerLogger.warn("Malformed packet received on session %s: %s", sessionId, e.getMessage());
                    Packet errorResponse = Packet.createErrorResponse("MALFORMED_FRAME", "err",
                            ResponseStatus.BAD_REQUEST, "Malformed frame: " + e.getMessage());
                    sendPacket(errorResponse);
                }
            }

        } catch (EOFException e) {
            ServerLogger.info("Client %s closed connection (EOF reached)", sessionId);
        } catch (SocketException e) {
            if (running) {
                ServerLogger.info("Socket connection reset/closed for session %s: %s", sessionId, e.getMessage());
            }
        } catch (IOException e) {
            if (running) {
                ServerLogger.error("I/O error on session " + sessionId, e);
            }
        } finally {
            close();
        }
    }

    /**
     * Sends a packet to this client safely.
     */
    public synchronized void sendPacket(Packet packet) throws IOException {
        if (!running || out == null || socket.isClosed()) {
            throw new IOException("Cannot send packet: session " + sessionId + " is closed");
        }
        ServerLogger.debug("Sending to session %s: type=%s, action=%s, reqId=%s, status=%s",
                sessionId, packet.getType(), packet.getAction(), packet.getRequestId(), packet.getStatus());
        ProtocolCodec.writePacket(out, packet);
    }

    /**
     * Associates an authenticated user identity with this connection.
     */
    public void setAuthenticatedUser(String userId) {
        this.authenticatedUserId = userId;
    }

    public void setUserRole(String role) {
        this.userRole = role;
    }

    public void clearAuthenticatedUser() {
        this.authenticatedUserId = null;
        this.userRole = null;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getAuthenticatedUserId() {
        return authenticatedUserId;
    }

    public String getUserRole() {
        return userRole;
    }

    public boolean isAuthenticated() {
        return authenticatedUserId != null;
    }

    public boolean isRunning() {
        return running && socket != null && !socket.isClosed();
    }

    public String getRemoteAddress() {
        if (socket != null && socket.getRemoteSocketAddress() != null) {
            return socket.getRemoteSocketAddress().toString();
        }
        return "unknown";
    }

    public long getConnectedAt() {
        return connectedAt;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Closes socket resources and cleans up session registration.
     */
    public void close() {
        if (!running) {
            return;
        }
        running = false;

        // Unregister from client manager
        if (clientManager != null) {
            clientManager.unregisterClient(this);
        }

        // Close streams and socket
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException ignored) {}

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException ignored) {}

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}

        ServerLogger.info("Session %s terminated and cleaned up", sessionId);
    }
}
