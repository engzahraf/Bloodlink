package com.bloodlink.util;

/**
 * Server configuration constants and defaults.
 */
public final class ServerConfig {

    public static final int DEFAULT_PORT = 9090;
    public static final int THREAD_POOL_SIZE = 50;
    public static final int SOCKET_TIMEOUT_MS = 60_000; // 60 seconds socket read timeout
    public static final int MAX_PACKET_LENGTH = 65_535; // Maximum UTF payload size allowed by DataOutputStream

    private ServerConfig() {
        // Constants class
    }
}
