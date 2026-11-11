package com.bloodlink.database.dao;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.enums.AccountStatus;
import com.bloodlink.database.enums.UserRole;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for User entities.
 */
public class UserDAO {

    /**
     * Inserts a new user into the database.
     *
     * @param user User entity to persist
     * @return User entity populated with the generated userId and timestamps
     * @throws ValidationException if email already exists or required fields are missing
     * @throws DatabaseException on SQL execution failure
     */
    public User createUser(User user) {
        if (user == null) {
            throw new ValidationException("User object cannot be null");
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            throw new ValidationException("Full name is required");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new ValidationException("Password hash is required");
        }
        if (user.getRole() == null) {
            throw new ValidationException("User role is required");
        }

        String sql = "INSERT INTO users (full_name, email, password_hash, phone, role, account_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getFullName().trim());
            stmt.setString(2, user.getEmail().trim().toLowerCase());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getPhone() != null ? user.getPhone().trim() : null);
            stmt.setString(5, user.getRole().name());
            stmt.setString(6, user.getAccountStatus() != null ? user.getAccountStatus().name() : AccountStatus.ACTIVE.name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating user failed, no ID obtained.");
                }
            }

            // Retrieve populated user with created_at/updated_at
            return getUserById(user.getUserId());
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: users.email")) {
                throw new ValidationException("A user with email '" + user.getEmail() + "' already exists.");
            }
            throw new DatabaseException("Error creating user: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a user by their unique primary key ID.
     */
    public User getUserById(int userId) {
        String sql = "SELECT user_id, full_name, email, password_hash, phone, role, account_status, " +
                     "created_at, updated_at FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving user by ID: " + userId, e);
        }
    }

    /**
     * Retrieves a user by their unique email address (case-insensitive).
     */
    public User getUserByEmail(String email) {
        if (email == null) return null;
        String sql = "SELECT user_id, full_name, email, password_hash, phone, role, account_status, " +
                     "created_at, updated_at FROM users WHERE LOWER(email) = LOWER(?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving user by email: " + email, e);
        }
    }

    /**
     * Retrieves all users registered in the system.
     */
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, full_name, email, password_hash, phone, role, account_status, " +
                     "created_at, updated_at FROM users ORDER BY user_id ASC";
        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all users", e);
        }
    }

    /**
     * Updates full_name, phone, role, and password_hash of an existing user.
     */
    public boolean updateUser(User user) {
        if (user == null || user.getUserId() <= 0) {
            throw new ValidationException("Valid user with ID is required for update");
        }

        String sql = "UPDATE users SET full_name = ?, phone = ?, role = ?, password_hash = ?, " +
                     "updated_at = datetime('now', 'localtime') WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName().trim());
            stmt.setString(2, user.getPhone() != null ? user.getPhone().trim() : null);
            stmt.setString(3, user.getRole().name());
            stmt.setString(4, user.getPasswordHash());
            stmt.setInt(5, user.getUserId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user: " + user.getUserId(), e);
        }
    }

    /**
     * Updates the account status of a user (ACTIVE or BLOCKED).
     */
    public boolean updateAccountStatus(int userId, AccountStatus status) {
        if (status == null) {
            throw new ValidationException("Account status cannot be null");
        }

        String sql = "UPDATE users SET account_status = ?, updated_at = datetime('now', 'localtime') WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating account status for user: " + userId, e);
        }
    }

    /**
     * Deletes a user by ID. Cascades to donor profiles, messages, and notifications.
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting user: " + userId, e);
        }
    }

    /**
     * Checks if an email is already registered in the system.
     */
    public boolean emailExists(String email) {
        if (email == null) return false;
        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error checking email existence: " + email, e);
        }
    }

    /**
     * Searches users across name, email, or phone (for admin management).
     */
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllUsers();
        }

        String pattern = "%" + query.trim().toLowerCase() + "%";
        String sql = "SELECT user_id, full_name, email, password_hash, phone, role, account_status, " +
                     "created_at, updated_at FROM users " +
                     "WHERE LOWER(full_name) LIKE ? OR LOWER(email) LIKE ? OR phone LIKE ? " +
                     "ORDER BY user_id ASC";
        List<User> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToUser(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching users with query: " + query, e);
        }
    }

    /**
     * Retrieves users filtered by role.
     */
    public List<User> getUsersByRole(UserRole role) {
        if (role == null) return getAllUsers();

        String sql = "SELECT user_id, full_name, email, password_hash, phone, role, account_status, " +
                     "created_at, updated_at FROM users WHERE role = ? ORDER BY user_id ASC";
        List<User> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToUser(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving users by role: " + role, e);
        }
    }

    /**
     * Retrieves aggregated user statistics for Admin dashboard.
     */
    public Map<String, Integer> getUserStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT " +
                     "COUNT(*) AS total_users, " +
                     "SUM(CASE WHEN role = 'DONOR' THEN 1 ELSE 0 END) AS total_donors, " +
                     "SUM(CASE WHEN role = 'RECIPIENT' THEN 1 ELSE 0 END) AS total_recipients, " +
                     "SUM(CASE WHEN role = 'ADMIN' THEN 1 ELSE 0 END) AS total_admins, " +
                     "SUM(CASE WHEN account_status = 'BLOCKED' THEN 1 ELSE 0 END) AS total_blocked " +
                     "FROM users";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.put("totalUsers", rs.getInt("total_users"));
                stats.put("totalDonors", rs.getInt("total_donors"));
                stats.put("totalRecipients", rs.getInt("total_recipients"));
                stats.put("totalAdmins", rs.getInt("total_admins"));
                stats.put("totalBlocked", rs.getInt("total_blocked"));
            }
            return stats;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving user statistics", e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("user_id"),
            rs.getString("full_name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("phone"),
            UserRole.fromString(rs.getString("role")),
            AccountStatus.fromString(rs.getString("account_status")),
            rs.getString("created_at"),
            rs.getString("updated_at")
        );
    }
}

