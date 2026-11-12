package com.bloodlink.database.dao;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.DonorResponse;

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
 * Data Access Object for Donor Responses (ACCEPTED, REJECTED, PENDING).
 */
public class DonorResponseDAO {

    /**
     * Records or updates a donor's response to a blood request.
     */
    public DonorResponse recordResponse(int requestId, int donorId, int recipientId, String status, String notes) {
        if (requestId <= 0) {
            throw new ValidationException("Valid requestId is required");
        }
        if (donorId <= 0) {
            throw new ValidationException("Valid donorId is required");
        }
        if (recipientId <= 0) {
            throw new ValidationException("Valid recipientId is required");
        }
        if (status == null || status.trim().isEmpty()) {
            throw new ValidationException("Response status cannot be empty");
        }

        String normStatus = status.trim().toUpperCase();
        if (!"ACCEPTED".equals(normStatus) && !"REJECTED".equals(normStatus) && !"PENDING".equals(normStatus)) {
            throw new ValidationException("Status must be ACCEPTED, REJECTED, or PENDING");
        }

        // Check for existing response
        String checkSql = "SELECT response_id, status FROM donor_responses WHERE request_id = ? AND donor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            int existingId = -1;
            String currentStatus = null;

            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, requestId);
                checkStmt.setInt(2, donorId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("response_id");
                        currentStatus = rs.getString("status");
                    }
                }
            }

            if (existingId > 0) {
                // If already has same status, return existing
                if (normStatus.equalsIgnoreCase(currentStatus)) {
                    return getResponseById(existingId);
                }

                // Update response
                String updateSql = "UPDATE donor_responses SET status = ?, notes = ?, updated_at = datetime('now', 'localtime') " +
                                   "WHERE response_id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, normStatus);
                    updateStmt.setString(2, notes != null ? notes.trim() : null);
                    updateStmt.setInt(3, existingId);
                    updateStmt.executeUpdate();
                }
                return getResponseById(existingId);
            } else {
                // Insert new response
                String insertSql = "INSERT INTO donor_responses (request_id, donor_id, recipient_id, status, notes) " +
                                   "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setInt(1, requestId);
                    insertStmt.setInt(2, donorId);
                    insertStmt.setInt(3, recipientId);
                    insertStmt.setString(4, normStatus);
                    insertStmt.setString(5, notes != null ? notes.trim() : null);

                    int affected = insertStmt.executeUpdate();
                    if (affected == 0) {
                        throw new DatabaseException("Failed to insert donor response.");
                    }

                    try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int newId = keys.getInt(1);
                            return getResponseById(newId);
                        } else {
                            throw new DatabaseException("Failed to obtain generated ID for donor response.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error recording donor response: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a response by its primary key ID.
     */
    public DonorResponse getResponseById(int responseId) {
        String sql = "SELECT dr.response_id, dr.request_id, dr.donor_id, dr.recipient_id, dr.status, " +
                     "dr.notes, dr.created_at, dr.updated_at, " +
                     "u.full_name AS donor_name, u.email AS donor_email, u.phone AS donor_phone, " +
                     "dp.blood_group, dp.location AS donor_location, dp.availability_status, dp.last_donation_date, " +
                     "br.hospital_name " +
                     "FROM donor_responses dr " +
                     "JOIN users u ON dr.donor_id = u.user_id " +
                     "LEFT JOIN donor_profiles dp ON dr.donor_id = dp.user_id " +
                     "LEFT JOIN blood_requests br ON dr.request_id = br.request_id " +
                     "WHERE dr.response_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, responseId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDonorResponse(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving donor response by ID: " + responseId, e);
        }
    }

    /**
     * Retrieves the response of a specific donor for a request.
     */
    public DonorResponse getResponse(int requestId, int donorId) {
        String sql = "SELECT dr.response_id, dr.request_id, dr.donor_id, dr.recipient_id, dr.status, " +
                     "dr.notes, dr.created_at, dr.updated_at, " +
                     "u.full_name AS donor_name, u.email AS donor_email, u.phone AS donor_phone, " +
                     "dp.blood_group, dp.location AS donor_location, dp.availability_status, dp.last_donation_date, " +
                     "br.hospital_name " +
                     "FROM donor_responses dr " +
                     "JOIN users u ON dr.donor_id = u.user_id " +
                     "LEFT JOIN donor_profiles dp ON dr.donor_id = dp.user_id " +
                     "LEFT JOIN blood_requests br ON dr.request_id = br.request_id " +
                     "WHERE dr.request_id = ? AND dr.donor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requestId);
            stmt.setInt(2, donorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDonorResponse(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving donor response: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all donor responses for a specific blood request.
     */
    public List<DonorResponse> getResponsesByRequest(int requestId) {
        String sql = "SELECT dr.response_id, dr.request_id, dr.donor_id, dr.recipient_id, dr.status, " +
                     "dr.notes, dr.created_at, dr.updated_at, " +
                     "u.full_name AS donor_name, u.email AS donor_email, u.phone AS donor_phone, " +
                     "dp.blood_group, dp.location AS donor_location, dp.availability_status, dp.last_donation_date, " +
                     "br.hospital_name " +
                     "FROM donor_responses dr " +
                     "JOIN users u ON dr.donor_id = u.user_id " +
                     "LEFT JOIN donor_profiles dp ON dr.donor_id = dp.user_id " +
                     "LEFT JOIN blood_requests br ON dr.request_id = br.request_id " +
                     "WHERE dr.request_id = ? ORDER BY dr.response_id DESC";
        List<DonorResponse> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDonorResponse(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving responses for request: " + requestId, e);
        }
    }

    /**
     * Retrieves all donor responses for all requests submitted by a recipient.
     */
    public List<DonorResponse> getResponsesByRecipient(int recipientId) {
        String sql = "SELECT dr.response_id, dr.request_id, dr.donor_id, dr.recipient_id, dr.status, " +
                     "dr.notes, dr.created_at, dr.updated_at, " +
                     "u.full_name AS donor_name, u.email AS donor_email, u.phone AS donor_phone, " +
                     "dp.blood_group, dp.location AS donor_location, dp.availability_status, dp.last_donation_date, " +
                     "br.hospital_name " +
                     "FROM donor_responses dr " +
                     "JOIN users u ON dr.donor_id = u.user_id " +
                     "LEFT JOIN donor_profiles dp ON dr.donor_id = dp.user_id " +
                     "LEFT JOIN blood_requests br ON dr.request_id = br.request_id " +
                     "WHERE dr.recipient_id = ? ORDER BY dr.response_id DESC";
        List<DonorResponse> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, recipientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDonorResponse(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving responses for recipient: " + recipientId, e);
        }
    }

    /**
     * Returns a map of requestId -> status for all responses by a specific donor.
     */
    public Map<Integer, String> getDonorResponsesMapForDonor(int donorId) {
        Map<Integer, String> map = new HashMap<>();
        String sql = "SELECT request_id, status FROM donor_responses WHERE donor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getInt("request_id"), rs.getString("status"));
                }
            }
            return map;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving donor responses map: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a map of requestId -> response count.
     */
    public Map<Integer, Integer> getResponseCountsByRequest() {
        Map<Integer, Integer> map = new HashMap<>();
        String sql = "SELECT request_id, COUNT(*) AS cnt FROM donor_responses GROUP BY request_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                map.put(rs.getInt("request_id"), rs.getInt("cnt"));
            }
            return map;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving response counts: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all donor responses across the system (for admin).
     */
    public List<DonorResponse> getAllResponses() {
        String sql = "SELECT dr.response_id, dr.request_id, dr.donor_id, dr.recipient_id, dr.status, " +
                     "dr.notes, dr.created_at, dr.updated_at, " +
                     "u.full_name AS donor_name, u.email AS donor_email, u.phone AS donor_phone, " +
                     "dp.blood_group, dp.location AS donor_location, dp.availability_status, dp.last_donation_date, " +
                     "br.hospital_name " +
                     "FROM donor_responses dr " +
                     "JOIN users u ON dr.donor_id = u.user_id " +
                     "LEFT JOIN donor_profiles dp ON dr.donor_id = dp.user_id " +
                     "LEFT JOIN blood_requests br ON dr.request_id = br.request_id " +
                     "ORDER BY dr.response_id DESC";
        List<DonorResponse> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToDonorResponse(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all donor responses: " + e.getMessage(), e);
        }
    }

    private DonorResponse mapRowToDonorResponse(ResultSet rs) throws SQLException {
        DonorResponse dr = new DonorResponse(
            rs.getInt("response_id"),
            rs.getInt("request_id"),
            rs.getInt("donor_id"),
            rs.getInt("recipient_id"),
            rs.getString("status"),
            rs.getString("notes"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        );
        dr.setDonorName(rs.getString("donor_name"));
        dr.setDonorEmail(rs.getString("donor_email"));
        dr.setDonorPhone(rs.getString("donor_phone"));
        dr.setDonorBloodGroup(rs.getString("blood_group"));
        dr.setDonorLocation(rs.getString("donor_location"));
        dr.setDonorAvailability(rs.getString("availability_status"));
        dr.setLastDonationDate(rs.getString("last_donation_date"));
        dr.setHospitalName(rs.getString("hospital_name"));
        return dr;
    }
}

