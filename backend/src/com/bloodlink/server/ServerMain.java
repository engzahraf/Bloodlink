package com.bloodlink.server;

import com.bloodlink.interfaces.IDatabaseService;
import com.bloodlink.services.AuthService;
import com.bloodlink.services.BloodMatchingService;
import com.bloodlink.services.MessagingService;
import com.bloodlink.services.NotificationService;
import com.bloodlink.util.ServerConfig;
import com.bloodlink.util.ServerLogger;

import java.io.IOException;

/**
 * Main application entry point for the BloodLink TCP Server.
 */
public class ServerMain {

    public static void main(String[] args) {
        int port = ServerConfig.DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0].trim());
            } catch (NumberFormatException e) {
                ServerLogger.warn("Invalid port argument '%s', using default port %d", args[0], port);
            }
        }

        ServerLogger.info("Initializing BloodLink Server environment on port %d...", port);

        // Core server components
        ClientManager clientManager = new ClientManager();

        // IDatabaseService can be supplied here once Member 03 completes the SQLite/JDBC module.
        // For standalone execution before database integration, a placeholder or null can be passed.
        IDatabaseService databaseService = null;

        NotificationService notificationService = new NotificationService(clientManager, databaseService);
        AuthService authService = new AuthService(databaseService, clientManager);
        BloodMatchingService bloodMatchingService = new BloodMatchingService(databaseService, clientManager, notificationService);
        MessagingService messagingService = new MessagingService(databaseService, clientManager);

        RequestDispatcher dispatcher = new RequestDispatcher(authService, bloodMatchingService, notificationService, messagingService);
        BloodLinkServer server = new BloodLinkServer(port, clientManager, dispatcher);

        try {
            server.start();
        } catch (IOException e) {
            ServerLogger.error("Failed to start BloodLink server: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
