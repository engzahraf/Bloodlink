package com.bloodlink.database.dao;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.enums.NotificationType;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user Notifications.
 */
public class NotificationDAO {

    /**
     * Creates and stores a new notification for a user.
     */
    public Notification createNotification(Notification notification) {
        if (notification == null) {
            throw new ValidationException("Notification cannot be null");
        }
        if (notification.getUserId() <= 0) {
            throw new ValidationException("Valid userId is required");
        }
        if (notification.getNotificationType() == null) {
            throw new ValidationException("NotificationType is required");
        }
        if (notification.getMessage() == null || notification.getMessage().trim().isEmpty()) {
            throw new ValidationException("Notification message cannot be empty");
        }

        String sql = "INSERT INTO notifications (user_id, notification_type, message, related_request_id, is_read) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, notification.getUserId());
            stmt.setString(2, notification.getNotificationType().name());
            stmt.setString(3, notification.getMessage().trim());

            if (notification.getRelatedRequestId() != null) {
                stmt.setInt(4, notification.getRelatedRequestId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }

            stmt.setInt(5, notification.isRead() ? 1 : 0);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Creating notification failed, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    notification.setNotificationId(keys.getInt(1));
                } else {
                    throw new DatabaseException("Creating notification failed, no ID obtained.");
                }
            }

            return getNotificationById(notification.getNotificationId());
        } catch (SQLException e) {
            throw new DatabaseException("Error creating notification: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a single notification by its ID.
     */
    public Notification getNotificationById(int notificationId) {
        String sql = "SELECT notification_id, user_id, notification_type, message, " +
                     "related_request_id, is_read, created_at FROM notifications WHERE notification_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, notificationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToNotification(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving notification: " + notificationId, e);
        }
    }

    /**
     * Retrieves all notifications for a specific user, sorted from newest to oldest.
     */
    public List<Notification> getNotificationsByUser(int userId) {
        String sql = "SELECT notification_id, user_id, notification_type, message, " +
                     "related_request_id, is_read, created_at FROM notifications " +
                     "WHERE user_id = ? ORDER BY notification_id DESC";
        List<Notification> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToNotification(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving notifications for user: " + userId, e);
        }
    }

    /**
     * Retrieves only unread notifications for a specific user.
     */
    public List<Notification> getUnreadNotifications(int userId) {
        String sql = "SELECT notification_id, user_id, notification_type, message, " +
                     "related_request_id, is_read, created_at FROM notifications " +
                     "WHERE user_id = ? AND is_read = 0 ORDER BY notification_id DESC";
        List<Notification> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToNotification(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving unread notifications for user: " + userId, e);
        }
    }

    /**
     * Marks a specific notification as read.
     */
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE notification_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error marking notification as read: " + notificationId, e);
        }
    }

    /**
     * Marks all unread notifications for a user as read.
     */
    public int markAllAsRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error marking all notifications as read for user: " + userId, e);
        }
    }

    /**
     * Retrieves all notifications across the platform (for Admin audit and system activity viewing).
     */
    public List<Notification> getAllNotifications() {
        String sql = "SELECT notification_id, user_id, notification_type, message, " +
                     "related_request_id, is_read, created_at FROM notifications " +
                     "ORDER BY notification_id DESC";
        List<Notification> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToNotification(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all notifications", e);
        }
    }

    /**
     * Retrieves the most recent system activity and notifications (limited to n entries).
     */
    public List<Notification> getRecentActivity(int limit) {
        int safeLimit = limit <= 0 ? 50 : limit;
        String sql = "SELECT notification_id, user_id, notification_type, message, " +
                     "related_request_id, is_read, created_at FROM notifications " +
                     "ORDER BY notification_id DESC LIMIT ?";
        List<Notification> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, safeLimit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToNotification(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving recent activity with limit: " + safeLimit, e);
        }
    }

    /**
     * Deletes a notification by its ID.
     */
    public boolean deleteNotification(int notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting notification: " + notificationId, e);
        }
    }

    private Notification mapRowToNotification(ResultSet rs) throws SQLException {
        int reqId = rs.getInt("related_request_id");
        Integer relatedReqId = rs.wasNull() ? null : reqId;

        return new Notification(
            rs.getInt("notification_id"),
            rs.getInt("user_id"),
            NotificationType.fromString(rs.getString("notification_type")),
            rs.getString("message"),
            relatedReqId,
            rs.getInt("is_read") == 1,
            rs.getString("created_at")
        );
    }
}

