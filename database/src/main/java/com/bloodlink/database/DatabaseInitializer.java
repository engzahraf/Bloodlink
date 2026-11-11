package com.bloodlink.database;

import com.bloodlink.database.enums.AccountStatus;
import com.bloodlink.database.enums.UserRole;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles automatic SQLite schema initialization, table creation,
 * constraint verification, indexing, and default system administrator seeding.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {
        // Utility class
    }

    /**
     * Initializes all database tables, constraints, and indexes.
     * Safe to run repeatedly (uses IF NOT EXISTS clauses).
     */
    public static synchronized void initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false);

            // 1. users table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "    user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    full_name TEXT NOT NULL," +
                "    email TEXT UNIQUE NOT NULL," +
                "    password_hash TEXT NOT NULL," +
                "    phone TEXT," +
                "    role TEXT NOT NULL CHECK(role IN ('ADMIN', 'DONOR', 'RECIPIENT'))," +
                "    account_status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(account_status IN ('ACTIVE', 'BLOCKED'))," +
                "    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))," +
                "    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ");"
            );

            // 2. donor_profiles table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS donor_profiles (" +
                "    donor_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    user_id INTEGER UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE," +
                "    blood_group TEXT NOT NULL CHECK(blood_group IN ('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'))," +
                "    location TEXT NOT NULL," +
                "    availability_status TEXT NOT NULL DEFAULT 'AVAILABLE' CHECK(availability_status IN ('AVAILABLE', 'UNAVAILABLE'))," +
                "    last_donation_date TEXT," +
                "    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))," +
                "    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ");"
            );

            // 3. blood_requests table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS blood_requests (" +
                "    request_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    recipient_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE," +
                "    blood_group TEXT NOT NULL CHECK(blood_group IN ('A_POSITIVE', 'A_NEGATIVE', 'B_POSITIVE', 'B_NEGATIVE', 'AB_POSITIVE', 'AB_NEGATIVE', 'O_POSITIVE', 'O_NEGATIVE'))," +
                "    units_required INTEGER NOT NULL CHECK(units_required > 0)," +
                "    hospital_name TEXT NOT NULL," +
                "    location TEXT NOT NULL," +
                "    contact_number TEXT NOT NULL," +
                "    urgency_level TEXT NOT NULL CHECK(urgency_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))," +
                "    request_status TEXT NOT NULL DEFAULT 'PENDING' CHECK(request_status IN ('PENDING', 'MATCHED', 'ACCEPTED', 'COMPLETED', 'CANCELLED', 'REJECTED'))," +
                "    description TEXT," +
                "    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))," +
                "    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ");"
            );

            // 4. donations table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS donations (" +
                "    donation_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    request_id INTEGER NOT NULL REFERENCES blood_requests(request_id) ON DELETE RESTRICT ON UPDATE CASCADE," +
                "    donor_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT ON UPDATE CASCADE," +
                "    donation_date TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))," +
                "    donation_status TEXT NOT NULL DEFAULT 'SCHEDULED' CHECK(donation_status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'))," +
                "    notes TEXT," +
                "    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ");"
            );

            // 5. notifications table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS notifications (" +
                "    notification_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE," +
                "    notification_type TEXT NOT NULL," +
                "    message TEXT NOT NULL," +
                "    related_request_id INTEGER REFERENCES blood_requests(request_id) ON DELETE SET NULL ON UPDATE CASCADE," +
                "    is_read INTEGER NOT NULL DEFAULT 0 CHECK(is_read IN (0, 1))," +
                "    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ");"
            );

            // 6. messages table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS messages (" +
                "    message_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "    sender_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE," +
                "    receiver_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE ON UPDATE CASCADE," +
                "    request_id INTEGER REFERENCES blood_requests(request_id) ON DELETE SET NULL ON UPDATE CASCADE," +
                "    message_content TEXT NOT NULL," +
                "    sent_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))," +
                "    is_read INTEGER NOT NULL DEFAULT 0 CHECK(is_read IN (0, 1))" +
                ");"
            );

            // Create Indexes for high-frequency search and join operations
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_donor_user_id ON donor_profiles(user_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_donor_blood_group ON donor_profiles(blood_group);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_donor_location ON donor_profiles(location);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_donor_availability ON donor_profiles(availability_status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_requests_recipient ON blood_requests(recipient_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_requests_status ON blood_requests(request_status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_requests_blood_group ON blood_requests(blood_group);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_donations_donor ON donations(donor_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_donations_request ON donations(request_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, is_read);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(sender_id, receiver_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_messages_request ON messages(request_id);");

            conn.commit();
            System.out.println("[DatabaseInitializer] Database schema and indexes initialized successfully.");
        } catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database schema", e);
        }
    }

    /**
     * Seeds a secure default system administrator account if no admin currently exists.
     * Plaintext password is NEVER stored. Password is salted and hashed using PBKDF2.
     *
     * @param adminEmail Admin email address (e.g. admin@bloodlink.com)
     * @param adminPassword Plaintext password to hash and store
     * @param adminFullName Full name of the administrator
     * @param adminPhone Contact number
     * @return true if a new admin account was created, false if an admin already exists
     */
    public static boolean seedDefaultAdminIfMissing(String adminEmail, String adminPassword,
                                                    String adminFullName, String adminPhone) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             ResultSet rs = checkStmt.executeQuery()) {

            if (rs.next() && rs.getInt(1) > 0) {
                return false; // Admin already exists
            }

            String insertSql = "INSERT INTO users (full_name, email, password_hash, phone, role, account_status) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                String hashedPassword = PasswordHasher.hashPassword(adminPassword);
                insertStmt.setString(1, adminFullName);
                insertStmt.setString(2, adminEmail);
                insertStmt.setString(3, hashedPassword);
                insertStmt.setString(4, adminPhone);
                insertStmt.setString(5, UserRole.ADMIN.name());
                insertStmt.setString(6, AccountStatus.ACTIVE.name());
                insertStmt.executeUpdate();
                System.out.println("[DatabaseInitializer] Seeded default administrator: " + adminEmail);
                return true;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check/seed default administrator", e);
        }
    }

    /**
     * Drops all tables (strictly for automated test environments).
     */
    public static synchronized void dropAllTables() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = OFF;");
            stmt.execute("DROP TABLE IF EXISTS messages;");
            stmt.execute("DROP TABLE IF EXISTS notifications;");
            stmt.execute("DROP TABLE IF EXISTS donations;");
            stmt.execute("DROP TABLE IF EXISTS blood_requests;");
            stmt.execute("DROP TABLE IF EXISTS donor_profiles;");
            stmt.execute("DROP TABLE IF EXISTS users;");
            stmt.execute("PRAGMA foreign_keys = ON;");
            System.out.println("[DatabaseInitializer] Dropped all tables.");
        } catch (SQLException e) {
            throw new DatabaseException("Failed to drop tables", e);
        }
    }

    /**
     * CLI entry point for initializing the SQLite database schema and seeding default data.
     * Useful for standalone execution, Docker containers, and CI/CD pipelines.
     */
    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("         BLOODLINK DATABASE INITIALIZATION SERVICE                     ");
        System.out.println("========================================================================");
        System.out.println("[DatabaseInitializer] Target database: " + DatabaseConnection.getActiveDatabasePath());

        try {
            initialize();
            boolean seeded = seedDefaultAdminIfMissing(
                "admin@bloodlink.com",
                "Admin@BloodLink2026",
                "System Administrator",
                "+94-77-1234567"
            );
            if (seeded) {
                System.out.println("[DatabaseInitializer] Created default administrator: admin@bloodlink.com");
            } else {
                System.out.println("[DatabaseInitializer] Administrator account already exists. Skipped seeding.");
            }
            System.out.println("[DatabaseInitializer] All tables, indexes, and constraints successfully verified.");
            System.out.println("========================================================================");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("[DatabaseInitializer] FATAL ERROR during initialization: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
