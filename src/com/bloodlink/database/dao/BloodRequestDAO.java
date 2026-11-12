package com.bloodlink.database.dao;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.enums.BloodGroup;
import com.bloodlink.database.enums.RequestStatus;
import com.bloodlink.database.enums.UrgencyLevel;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.BloodRequest;

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
 * Data Access Object for BloodRequest entities.
 */
public class BloodRequestDAO {

    /**
     * Inserts a new blood request into the database.
     */
    public BloodRequest createBloodRequest(BloodRequest request) {
        if (request == null) {
            throw new ValidationException("BloodRequest cannot be null");
        }
        if (request.getRecipientId() <= 0) {
            throw new ValidationException("Valid recipientId is required");
        }
        if (request.getBloodGroup() == null) {
            throw new ValidationException("Blood group is required");
        }
        if (request.getUnitsRequired() <= 0) {
            throw new ValidationException("Units required must be greater than zero");
        }
        if (request.getHospitalName() == null || request.getHospitalName().trim().isEmpty()) {
            throw new ValidationException("Hospital name is required");
        }
        if (request.getLocation() == null || request.getLocation().trim().isEmpty()) {
            throw new ValidationException("Location is required");
        }
        if (request.getContactNumber() == null || request.getContactNumber().trim().isEmpty()) {
            throw new ValidationException("Contact number is required");
        }

        String sql = "INSERT INTO blood_requests (recipient_id, blood_group, units_required, " +
                     "hospital_name, location, contact_number, urgency_level, request_status, description) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, request.getRecipientId());
            stmt.setString(2, request.getBloodGroup().name());
            stmt.setInt(3, request.getUnitsRequired());
            stmt.setString(4, request.getHospitalName().trim());
            stmt.setString(5, request.getLocation().trim());
            stmt.setString(6, request.getContactNumber().trim());
            stmt.setString(7, request.getUrgencyLevel() != null ? request.getUrgencyLevel().name() : UrgencyLevel.MEDIUM.name());
            stmt.setString(8, request.getRequestStatus() != null ? request.getRequestStatus().name() : RequestStatus.PENDING.name());
            stmt.setString(9, request.getDescription() != null ? request.getDescription().trim() : null);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating blood request failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    request.setRequestId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating blood request failed, no ID obtained.");
                }
            }

            return getRequestById(request.getRequestId());
        } catch (SQLException e) {
            throw new DatabaseException("Error creating blood request: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a blood request by its primary key ID.
     */
    public BloodRequest getRequestById(int requestId) {
        String sql = "SELECT br.request_id, br.recipient_id, br.blood_group, br.units_required, " +
                     "br.hospital_name, br.location, br.contact_number, br.urgency_level, " +
                     "br.request_status, br.description, br.created_at, br.updated_at, " +
                     "u.full_name AS recipient_name, u.email AS recipient_email " +
                     "FROM blood_requests br " +
                     "JOIN users u ON br.recipient_id = u.user_id " +
                     "WHERE br.request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBloodRequest(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving blood request by ID: " + requestId, e);
        }
    }

    /**
     * Retrieves all blood requests ordered by creation time descending.
     */
    public List<BloodRequest> getAllBloodRequests() {
        String sql = "SELECT br.request_id, br.recipient_id, br.blood_group, br.units_required, " +
                     "br.hospital_name, br.location, br.contact_number, br.urgency_level, " +
                     "br.request_status, br.description, br.created_at, br.updated_at, " +
                     "u.full_name AS recipient_name, u.email AS recipient_email " +
                     "FROM blood_requests br " +
                     "JOIN users u ON br.recipient_id = u.user_id " +
                     "ORDER BY br.request_id DESC";
        List<BloodRequest> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToBloodRequest(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all blood requests", e);
        }
    }

    /**
     * Retrieves all blood requests created by a specific recipient.
     */
    public List<BloodRequest> getRequestsByRecipient(int recipientId) {
        String sql = "SELECT br.request_id, br.recipient_id, br.blood_group, br.units_required, " +
                     "br.hospital_name, br.location, br.contact_number, br.urgency_level, " +
                     "br.request_status, br.description, br.created_at, br.updated_at, " +
                     "u.full_name AS recipient_name, u.email AS recipient_email " +
                     "FROM blood_requests br " +
                     "JOIN users u ON br.recipient_id = u.user_id " +
                     "WHERE br.recipient_id = ? " +
                     "ORDER BY br.request_id DESC";
        List<BloodRequest> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, recipientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBloodRequest(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving requests for recipient: " + recipientId, e);
        }
    }

    /**
     * Retrieves blood requests by status (e.g. PENDING, MATCHED, etc.).
     */
    public List<BloodRequest> getRequestsByStatus(RequestStatus status) {
        if (status == null) return getAllBloodRequests();

        String sql = "SELECT br.request_id, br.recipient_id, br.blood_group, br.units_required, " +
                     "br.hospital_name, br.location, br.contact_number, br.urgency_level, " +
                     "br.request_status, br.description, br.created_at, br.updated_at, " +
                     "u.full_name AS recipient_name, u.email AS recipient_email " +
                     "FROM blood_requests br " +
                     "JOIN users u ON br.recipient_id = u.user_id " +
                     "WHERE br.request_status = ? " +
                     "ORDER BY br.request_id DESC";
        List<BloodRequest> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBloodRequest(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving blood requests by status: " + status, e);
        }
    }

    /**
     * Retrieves blood requests matching a specific blood group.
     */
    public List<BloodRequest> searchRequestsByBloodGroup(BloodGroup bloodGroup) {
        if (bloodGroup == null) return getAllBloodRequests();

        String sql = "SELECT br.request_id, br.recipient_id, br.blood_group, br.units_required, " +
                     "br.hospital_name, br.location, br.contact_number, br.urgency_level, " +
                     "br.request_status, br.description, br.created_at, br.updated_at, " +
                     "u.full_name AS recipient_name, u.email AS recipient_email " +
                     "FROM blood_requests br " +
                     "JOIN users u ON br.recipient_id = u.user_id " +
                     "WHERE br.blood_group = ? " +
                     "ORDER BY br.request_id DESC";
        List<BloodRequest> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bloodGroup.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToBloodRequest(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving blood requests by blood group: " + bloodGroup, e);
        }
    }

    /**
     * Updates an existing blood request details.
     */
    public boolean updateBloodRequest(BloodRequest request) {
        if (request == null || request.getRequestId() <= 0) {
            throw new ValidationException("Valid blood request with ID is required");
        }

        String sql = "UPDATE blood_requests SET blood_group = ?, units_required = ?, " +
                     "hospital_name = ?, location = ?, contact_number = ?, urgency_level = ?, " +
                     "description = ?, updated_at = datetime('now', 'localtime') " +
                     "WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, request.getBloodGroup().name());
            stmt.setInt(2, request.getUnitsRequired());
            stmt.setString(3, request.getHospitalName().trim());
            stmt.setString(4, request.getLocation().trim());
            stmt.setString(5, request.getContactNumber().trim());
            stmt.setString(6, request.getUrgencyLevel().name());
            stmt.setString(7, request.getDescription() != null ? request.getDescription().trim() : null);
            stmt.setInt(8, request.getRequestId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating blood request: " + request.getRequestId(), e);
        }
    }

    /**
     * Updates the status of a blood request.
     */
    public boolean updateRequestStatus(int requestId, RequestStatus status) {
        if (status == null) {
            throw new ValidationException("RequestStatus cannot be null");
        }

        String sql = "UPDATE blood_requests SET request_status = ?, updated_at = datetime('now', 'localtime') " +
                     "WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, requestId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating blood request status: " + requestId, e);
        }
    }

    /**
     * Cancels a blood request (sets status to CANCELLED).
     */
    public boolean cancelBloodRequest(int requestId) {
        return updateRequestStatus(requestId, RequestStatus.CANCELLED);
    }

    /**
     * Deletes a blood request.
     */
    public boolean deleteBloodRequest(int requestId) {
        String sql = "DELETE FROM blood_requests WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requestId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting blood request: " + requestId, e);
        }
    }

    /**
     * Retrieves aggregated blood request statistics for Admin dashboard.
     */
    public Map<String, Integer> getRequestStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT " +
                     "COUNT(*) AS total_requests, " +
                     "SUM(CASE WHEN request_status = 'PENDING' THEN 1 ELSE 0 END) AS pending_requests, " +
                     "SUM(CASE WHEN request_status = 'MATCHED' THEN 1 ELSE 0 END) AS matched_requests, " +
                     "SUM(CASE WHEN request_status = 'ACCEPTED' THEN 1 ELSE 0 END) AS accepted_requests, " +
                     "SUM(CASE WHEN request_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_requests, " +
                     "SUM(CASE WHEN request_status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelled_requests, " +
                     "SUM(CASE WHEN urgency_level = 'CRITICAL' THEN 1 ELSE 0 END) AS critical_requests " +
                     "FROM blood_requests";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                stats.put("totalRequests", rs.getInt("total_requests"));
                stats.put("pendingRequests", rs.getInt("pending_requests"));
                stats.put("matchedRequests", rs.getInt("matched_requests"));
                stats.put("acceptedRequests", rs.getInt("accepted_requests"));
                stats.put("completedRequests", rs.getInt("completed_requests"));
                stats.put("cancelledRequests", rs.getInt("cancelled_requests"));
                stats.put("criticalRequests", rs.getInt("critical_requests"));
            }
            return stats;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving request statistics", e);
        }
    }

    /**
     * Retrieves counts of blood requests aggregated by blood group.
     */
    public Map<String, Integer> getBloodGroupCounts() {
        Map<String, Integer> counts = new HashMap<>();
        String sql = "SELECT blood_group, COUNT(*) as cnt FROM blood_requests GROUP BY blood_group";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getString("blood_group"), rs.getInt("cnt"));
            }
            return counts;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving blood group counts: " + e.getMessage(), e);
        }
    }

    private BloodRequest mapRowToBloodRequest(ResultSet rs) throws SQLException {
        BloodRequest req = new BloodRequest(
            rs.getInt("request_id"),
            rs.getInt("recipient_id"),
            BloodGroup.fromString(rs.getString("blood_group")),
            rs.getInt("units_required"),
            rs.getString("hospital_name"),
            rs.getString("location"),
            rs.getString("contact_number"),
            UrgencyLevel.fromString(rs.getString("urgency_level")),
            RequestStatus.fromString(rs.getString("request_status")),
            rs.getString("description"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        );
        req.setRecipientName(rs.getString("recipient_name"));
        req.setRecipientEmail(rs.getString("recipient_email"));
        return req;
    }
}

