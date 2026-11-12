package com.bloodlink.database;

import com.bloodlink.database.exceptions.DatabaseException;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite JDBC database connections for BloodLink.
 * Ensures thread-safe connection vending, foreign key enforcement,
 * WAL journal mode, and busy timeout settings for concurrent access.
 */
public final class DatabaseConnection {

    private static final String DEFAULT_DB_DIR = "data";
    private static final String DEFAULT_DB_NAME = "bloodlink.db";
    private static final String DEFAULT_DB_PATH = resolveInitialDbPath();

    private static volatile String activeDbPath = DEFAULT_DB_PATH;
    private static volatile boolean isInitialized = false;

    private static String resolveInitialDbPath() {
        String envPath = System.getenv("BLOODLINK_DB_PATH");
        if (envPath != null && !envPath.trim().isEmpty()) {
            return envPath.trim();
        }
        String sysProp = System.getProperty("bloodlink.db.path");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp.trim();
        }
        return DEFAULT_DB_DIR + File.separator + DEFAULT_DB_NAME;
    }

    static {
        try {
            // Explicitly load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] SQLite JDBC driver not found: " + e.getMessage());
        }
    }

    private DatabaseConnection() {
        // Utility class
    }

    /**
     * Sets a custom database path (useful for testing).
     *
     * @param dbPath Path to the SQLite database file
     */
    public static synchronized void setDatabasePath(String dbPath) {
        activeDbPath = dbPath;
        isInitialized = false;
    }

    /**
     * Resets the database path back to the production default.
     */
    public static synchronized void resetToDefaultDatabasePath() {
        activeDbPath = DEFAULT_DB_PATH;
        isInitialized = false;
    }

    /**
     * Returns the active database path.
     */
    public static String getActiveDatabasePath() {
        return activeDbPath;
    }

    /**
     * Gets a fresh, properly configured connection to the SQLite database.
     * Each caller receives its own Connection instance to prevent cross-thread contention.
     * Callers MUST close this connection (ideally using try-with-resources).
     *
     * @return Configured java.sql.Connection
     * @throws DatabaseException if connection cannot be established
     */
    public static Connection getConnection() {
        ensureDirectoryExists(activeDbPath);

        String url = "jdbc:sqlite:" + activeDbPath;
        try {
            Connection connection = DriverManager.getConnection(url);
            configurePragmas(connection);
            return connection;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to establish SQLite connection to: " + url, e);
        }
    }

    /**
     * Configures essential SQLite PRAGMAs for safety and high concurrency:
     * - foreign_keys = ON: Enforces referential integrity on every connection.
     * - journal_mode = WAL: Write-Ahead Logging allows simultaneous readers and writers.
     * - busy_timeout = 5000: Prevents immediate SQLITE_BUSY exceptions under concurrent load.
     * - synchronous = NORMAL: Recommended with WAL for high performance and durability.
     */
    private static void configurePragmas(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
        }
    }

    /**
     * Creates parent directory if it does not already exist.
     */
    private static void ensureDirectoryExists(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (!created && !parent.exists()) {
                System.err.println("[DatabaseConnection] Warning: Could not create directory: " + parent.getAbsolutePath());
            }
        }
    }
}

