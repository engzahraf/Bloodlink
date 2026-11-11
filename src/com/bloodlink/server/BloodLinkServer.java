package com.bloodlink.server;

import com.bloodlink.util.ServerConfig;
import com.bloodlink.util.ServerLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core TCP Server for the BloodLink System.
 * Manages ServerSocket lifecycle, accepts multiple concurrent client connections,
 * and delegates each connection to a ClientHandler in an ExecutorService thread pool.
 */
public class BloodLinkServer {

    private final int configuredPort;
    private ServerSocket serverSocket;
    private final ClientManager clientManager;
    private final RequestDispatcher requestDispatcher;
    private ExecutorService threadPool;

    private volatile boolean running = false;
    private Thread serverAcceptThread;
    private Thread shutdownHookThread;

    public BloodLinkServer() {
        this(ServerConfig.DEFAULT_PORT, new ClientManager(), new RequestDispatcher());
    }

    public BloodLinkServer(int port) {
        this(port, new ClientManager(), new RequestDispatcher());
    }

    public BloodLinkServer(int port, ClientManager clientManager, RequestDispatcher requestDispatcher) {
        this.configuredPort = port;
        this.clientManager = clientManager != null ? clientManager : new ClientManager();
        this.requestDispatcher = requestDispatcher != null ? requestDispatcher : new RequestDispatcher();
    }

    /**
     * Starts the server synchronously on the current thread (blocks on accept loop).
     */
    public void start() throws IOException {
        initialize();
        ServerLogger.info("BloodLink Server listening on port %d...", getPort());
        runAcceptLoop();
    }

    /**
     * Starts the server asynchronously on a dedicated accept thread.
     */
    public synchronized void startAsync() throws IOException {
        initialize();
        serverAcceptThread = new Thread(this::runAcceptLoop, "bloodlink-acceptor");
        serverAcceptThread.start();
        ServerLogger.info("BloodLink Server started asynchronously on port %d", getPort());
    }

    private synchronized void initialize() throws IOException {
        if (running) {
            throw new IllegalStateException("Server is already running on port " + getPort());
        }

        threadPool = Executors.newFixedThreadPool(ServerConfig.THREAD_POOL_SIZE, new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "bloodlink-worker-" + count.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        });

        serverSocket = new ServerSocket(configuredPort);
        running = true;

        // Register JVM shutdown hook for graceful termination
        shutdownHookThread = new Thread(this::stop, "bloodlink-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHookThread);
    }

    private void runAcceptLoop() {
        while (running && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, clientManager, requestDispatcher);
                clientManager.registerClient(handler);
                threadPool.submit(handler);
            } catch (SocketException e) {
                if (!running) {
                    ServerLogger.info("ServerSocket closed during server shutdown");
                    break;
                }
                ServerLogger.error("SocketException in accept loop", e);
            } catch (IOException e) {
                if (running) {
                    ServerLogger.error("IOException in accept loop", e);
                }
            }
        }
    }

    /**
     * Gracefully stops the server, closes all sockets, and terminates the thread pool.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        ServerLogger.info("Initiating BloodLink Server shutdown...");

        // Unregister shutdown hook if stopping programmatically
        if (shutdownHookThread != null && Thread.currentThread() != shutdownHookThread) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
            } catch (IllegalStateException ignored) {
                // JVM may already be shutting down
            }
        }

        // Close ServerSocket to unblock accept loop
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            ServerLogger.warn("Error closing server socket: %s", e.getMessage());
        }

        // Disconnect all active client connections safely
        if (clientManager != null) {
            clientManager.disconnectAll("Server shutdown");
        }

        // Shutdown thread pool
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(3, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        ServerLogger.info("BloodLink Server stopped successfully.");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return (serverSocket != null && !serverSocket.isClosed()) ? serverSocket.getLocalPort() : configuredPort;
    }

    public ClientManager getClientManager() {
        return clientManager;
    }

    public RequestDispatcher getRequestDispatcher() {
        return requestDispatcher;
    }
}
