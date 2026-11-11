package com.bloodlink.database.dao;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Donor-Recipient direct messaging.
 */
public class MessageDAO {

    /**
     * Saves a new message to the database.
     */
    public Message saveMessage(Message message) {
        if (message == null) {
            throw new ValidationException("Message cannot be null");
        }
        if (message.getSenderId() <= 0) {
            throw new ValidationException("Valid senderId is required");
        }
        if (message.getReceiverId() <= 0) {
            throw new ValidationException("Valid receiverId is required");
        }
        if (message.getMessageContent() == null || message.getMessageContent().trim().isEmpty()) {
            throw new ValidationException("Message content cannot be empty");
        }

        String sql = "INSERT INTO messages (sender_id, receiver_id, request_id, message_content, is_read) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, message.getSenderId());
            stmt.setInt(2, message.getReceiverId());

            if (message.getRequestId() != null) {
                stmt.setInt(3, message.getRequestId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }

            stmt.setString(4, message.getMessageContent().trim());
            stmt.setInt(5, message.isRead() ? 1 : 0);

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new DatabaseException("Failed to save message, no rows affected.");
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    message.setMessageId(keys.getInt(1));
                } else {
                    throw new DatabaseException("Failed to obtain message ID.");
                }
            }

            return getMessageById(message.getMessageId());
        } catch (SQLException e) {
            throw new DatabaseException("Error saving message: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a single message by ID.
     */
    public Message getMessageById(int messageId) {
        String sql = "SELECT m.message_id, m.sender_id, m.receiver_id, m.request_id, " +
                     "m.message_content, m.sent_at, m.is_read, " +
                     "s.full_name AS sender_name, r.full_name AS receiver_name " +
                     "FROM messages m " +
                     "JOIN users s ON m.sender_id = s.user_id " +
                     "JOIN users r ON m.receiver_id = r.user_id " +
                     "WHERE m.message_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMessage(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving message by ID: " + messageId, e);
        }
    }

    /**
     * Retrieves the complete chronological chat history between two specific users.
     */
    public List<Message> getConversation(int user1Id, int user2Id) {
        String sql = "SELECT m.message_id, m.sender_id, m.receiver_id, m.request_id, " +
                     "m.message_content, m.sent_at, m.is_read, " +
                     "s.full_name AS sender_name, r.full_name AS receiver_name " +
                     "FROM messages m " +
                     "JOIN users s ON m.sender_id = s.user_id " +
                     "JOIN users r ON m.receiver_id = r.user_id " +
                     "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
                     "ORDER BY m.message_id ASC";
        List<Message> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, user1Id);
            stmt.setInt(2, user2Id);
            stmt.setInt(3, user2Id);
            stmt.setInt(4, user1Id);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving conversation between users: " + user1Id + " and " + user2Id, e);
        }
    }

    /**
     * Retrieves all messages exchanged regarding a specific blood request ID.
     */
    public List<Message> getMessagesByRequest(int requestId) {
        String sql = "SELECT m.message_id, m.sender_id, m.receiver_id, m.request_id, " +
                     "m.message_content, m.sent_at, m.is_read, " +
                     "s.full_name AS sender_name, r.full_name AS receiver_name " +
                     "FROM messages m " +
                     "JOIN users s ON m.sender_id = s.user_id " +
                     "JOIN users r ON m.receiver_id = r.user_id " +
                     "WHERE m.request_id = ? " +
                     "ORDER BY m.message_id ASC";
        List<Message> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving messages for request: " + requestId, e);
        }
    }

    /**
     * Retrieves all unread messages received by a specific user.
     */
    public List<Message> getUnreadMessages(int userId) {
        String sql = "SELECT m.message_id, m.sender_id, m.receiver_id, m.request_id, " +
                     "m.message_content, m.sent_at, m.is_read, " +
                     "s.full_name AS sender_name, r.full_name AS receiver_name " +
                     "FROM messages m " +
                     "JOIN users s ON m.sender_id = s.user_id " +
                     "JOIN users r ON m.receiver_id = r.user_id " +
                     "WHERE m.receiver_id = ? AND m.is_read = 0 " +
                     "ORDER BY m.message_id ASC";
        List<Message> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToMessage(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving unread messages for user: " + userId, e);
        }
    }

    /**
     * Marks a specific message as read.
     */
    public boolean markMessageAsRead(int messageId) {
        String sql = "UPDATE messages SET is_read = 1 WHERE message_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error marking message as read: " + messageId, e);
        }
    }

    /**
     * Deletes a message by its ID.
     */
    public boolean deleteMessage(int messageId) {
        String sql = "DELETE FROM messages WHERE message_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, messageId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting message: " + messageId, e);
        }
    }

    private Message mapRowToMessage(ResultSet rs) throws SQLException {
        int reqId = rs.getInt("request_id");
        Integer requestId = rs.wasNull() ? null : reqId;

        Message msg = new Message(
            rs.getInt("message_id"),
            rs.getInt("sender_id"),
            rs.getInt("receiver_id"),
            requestId,
            rs.getString("message_content"),
            rs.getString("sent_at"),
            rs.getInt("is_read") == 1
        );
        msg.setSenderName(rs.getString("sender_name"));
        msg.setReceiverName(rs.getString("receiver_name"));
        return msg;
    }
}

