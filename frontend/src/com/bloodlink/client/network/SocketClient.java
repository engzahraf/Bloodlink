package com.bloodlink.client.network;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Low-level TCP socket client handling connection lifecycle and single-reader stream I/O.
 * Communicates using DataOutputStream.writeUTF() and DataInputStream.readUTF().
 */
public class SocketClient {

    public interface SocketListener {
        void onPacketReceived(String rawWireString);
        void onConnected();
        void onDisconnected(String reason);
        void onError(Throwable t);
    }

    private final String host;
    private final int port;
    private final SocketListener listener;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    private Thread readerThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Object writeLock = new Object();

    public SocketClient(String host, int port, SocketListener listener) {
        this.host = host != null ? host : "127.0.0.1";
        this.port = port > 0 ? port : 5000;
        this.listener = listener;
    }

    /**
     * Connects to the TCP server using a 5-second socket connection timeout.
     */
    public synchronized void connect() throws IOException {
        if (isConnected()) {
            return;
        }

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
        socket.setTcpNoDelay(true); // Disable Nagle's algorithm for interactive responsiveness
        socket.setKeepAlive(true);

        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        isRunning.set(true);

        // Start dedicated background reader thread (single consumer)
        readerThread = new Thread(this::readLoop, "BloodLink-SocketReader");
        readerThread.setDaemon(true);
        readerThread.start();

        if (listener != null) {
            listener.onConnected();
        }
    }

    private void readLoop() {
        while (isRunning.get() && socket != null && !socket.isClosed()) {
            try {
                String rawPacket = in.readUTF();
                if (listener != null) {
                    listener.onPacketReceived(rawPacket);
                }
            } catch (EOFException e) {
                // Server closed socket cleanly
                handleDisconnect("Server closed connection (EOF)");
                break;
            } catch (IOException e) {
                if (isRunning.get()) {
                    handleDisconnect("Connection lost: " + e.getMessage());
                }
                break;
            } catch (Throwable t) {
                if (listener != null) {
                    listener.onError(t);
                }
            }
        }
    }

    /**
     * Sends a packet wire string safely across threads using a dedicated write lock.
     */
    public void send(String wireString) throws IOException {
        if (!isConnected()) {
            throw new IOException("Socket is not connected to " + host + ":" + port);
        }

        synchronized (writeLock) {
            out.writeUTF(wireString);
            out.flush();
        }
    }

    public synchronized void disconnect() {
        if (!isRunning.getAndSet(false)) {
            return;
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        } finally {
            socket = null;
            out = null;
            in = null;
        }

        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
        }
    }

    private void handleDisconnect(String reason) {
        disconnect();
        if (listener != null) {
            listener.onDisconnected(reason);
        }
    }

    public boolean isConnected() {
        return isRunning.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
