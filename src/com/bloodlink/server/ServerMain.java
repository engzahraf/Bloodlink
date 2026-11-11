package com.bloodlink.server;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.DatabaseInitializer;
import com.bloodlink.database.SQLiteDatabaseService;
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
 * Integrates SQLite database initialization, dependency injection, and TCP listener startup.
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

        ServerLogger.info("=================================================================");
        ServerLogger.info("          BLOODLINK TCP SERVER & BACKEND STARTUP                 ");
        ServerLogger.info("=================================================================");
        ServerLogger.info("Active SQLite database file: %s", DatabaseConnection.getActiveDatabasePath());

        // 1. Initialize SQLite Database Schema, Tables, and Indexes
        try {
            DatabaseInitializer.initialize();
            ServerLogger.info("SQLite database schema and indexes initialized successfully.");

            // 2. Seed Administrator account ONLY when explicitly enabled through BLOODLINK_SEED_ADMIN=true
            // and valid BLOODLINK_ADMIN_USER and BLOODLINK_ADMIN_PASS environment variables are supplied.
            String seedEnabled = System.getenv("BLOODLINK_SEED_ADMIN");
            if ("true".equalsIgnoreCase(seedEnabled != null ? seedEnabled.trim() : null)) {
                String adminUser = System.getenv("BLOODLINK_ADMIN_USER");
                if (adminUser == null || adminUser.trim().isEmpty()) {
                    adminUser = System.getenv("BLOODLINK_ADMIN_EMAIL");
                }
                String adminPass = System.getenv("BLOODLINK_ADMIN_PASS");
                if (adminPass == null || adminPass.trim().isEmpty()) {
                    adminPass = System.getenv("BLOODLINK_ADMIN_PASSWORD");
                }

                if (adminUser != null && !adminUser.trim().isEmpty() && adminPass != null && !adminPass.trim().isEmpty()) {
                    if (adminPass.length() < 8) {
                        ServerLogger.warn("Admin seeding skipped: BLOODLINK_ADMIN_PASS must be at least 8 characters long.");
                    } else {
                        boolean seeded = DatabaseInitializer.seedDefaultAdminIfMissing(
                                adminUser.trim().toLowerCase(),
                                adminPass,
                                "System Administrator",
                                ""
                        );
                        if (seeded) {
                            ServerLogger.info("Administrator account seeded successfully for: %s", adminUser.trim().toLowerCase());
                        } else {
                            ServerLogger.info("Administrator account already exists in database; seeding skipped.");
                        }
                    }
                } else {
                    ServerLogger.warn("BLOODLINK_SEED_ADMIN=true was set, but BLOODLINK_ADMIN_USER or BLOODLINK_ADMIN_PASS was not provided. Seeding skipped.");
                }
            } else {
                ServerLogger.info("Admin seeding is disabled (BLOODLINK_SEED_ADMIN != true).");
            }
        } catch (Exception e) {
            ServerLogger.error("Failed to initialize SQLite database: " + e.getMessage(), e);
            System.exit(1);
        }

        // 3. Instantiate Real Database Service & Server Components
        ClientManager clientManager = new ClientManager();
        IDatabaseService databaseService = new SQLiteDatabaseService();

        NotificationService notificationService = new NotificationService(clientManager, databaseService);
        AuthService authService = new AuthService(databaseService, clientManager);
        BloodMatchingService bloodMatchingService = new BloodMatchingService(databaseService, clientManager, notificationService);
        MessagingService messagingService = new MessagingService(databaseService, clientManager);

        RequestDispatcher dispatcher = new RequestDispatcher(authService, bloodMatchingService, notificationService, messagingService);
        BloodLinkServer server = new BloodLinkServer(port, clientManager, dispatcher);

        // 4. Start TCP ServerSocket Listener
        try {
            server.start();
        } catch (IOException e) {
            ServerLogger.error("Failed to start BloodLink TCP server: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
