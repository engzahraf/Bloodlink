package com.bloodlink.client;

import com.bloodlink.client.network.NetworkManager;
import com.bloodlink.client.ui.NavigationManager;
import com.bloodlink.client.ui.ThemeManager;
import com.bloodlink.client.utils.SessionManager;

import javax.swing.*;

/**
 * BloodLink Client Application Main Entry Point.
 *
 * Command line options:
 *   --mock          Starts application directly in Offline Mock Mode
 *   --host <ip>     Custom TCP server host (default: 127.0.0.1)
 *   --port <port>   Custom TCP server port (default: 9090)
 */
public class Main {

    public static void main(String[] args) {
        // Parse CLI parameters
        boolean startInMock = false;
        String host = "127.0.0.1";
        int port = 9090;

        for (int i = 0; i < args.length; i++) {
            if ("--mock".equalsIgnoreCase(args[i])) {
                startInMock = true;
            } else if ("--host".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                host = args[++i];
            } else if ("--port".equalsIgnoreCase(args[i]) && i + 1 < args.length) {
                try {
                    port = Integer.parseInt(args[++i]);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        SessionManager.getInstance().setMockMode(startInMock);
        SessionManager.getInstance().setServerHost(host);
        SessionManager.getInstance().setServerPort(port);

        final boolean finalMock = startInMock;

        // Safe JVM shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[BloodLink] Shutting down client application safely...");
            NetworkManager.getInstance().disconnect();
        }, "BloodLink-ShutdownHook"));

        // Initialize GUI on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                ThemeManager.initializeLookAndFeel();
                NavigationManager navigationManager = new NavigationManager();
                navigationManager.showLogin();
                System.out.println("[BloodLink] Desktop client UI launched successfully (Mock Mode: " + finalMock + ")");
            } catch (Throwable t) {
                System.err.println("[BloodLink] Fatal error initializing client UI: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }
}
