package com.bloodlink.database.dao;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.enums.BloodGroup;
import com.bloodlink.database.enums.DonationStatus;
import com.bloodlink.database.enums.RequestStatus;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.Donation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Donation records with transactional guarantees.
 */
public class DonationDAO {

    /**
     * Records a new blood donation within an atomic database transaction.
     * When completed, it synchronizes request status and donor profile dates.
     */
    public Donation recordDonation(Donation donation) {
        if (donation == null) {
            throw new ValidationException("Donation object cannot be null");
        }
        if (donation.getRequestId() <= 0) {
            throw new ValidationException("Valid requestId is required");
        }
        if (donation.getDonorId() <= 0) {
            throw new ValidationException("Valid donorId (user ID) is required");
        }

        String insertSql = "INSERT INTO donations (request_id, donor_id, donation_date, donation_status, notes) " +
                           "VALUES (?, ?, COALESCE(?, datetime('now', 'localtime')), ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            int generatedDonationId;
            try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, donation.getRequestId());
                stmt.setInt(2, donation.getDonorId());
                stmt.setString(3, donation.getDonationDate());
                stmt.setString(4, donation.getDonationStatus() != null ? donation.getDonationStatus().name() : DonationStatus.SCHEDULED.name());
                stmt.setString(5, donation.getNotes() != null ? donation.getNotes().trim() : null);

                int affected = stmt.executeUpdate();
                if (affected == 0) {
                    throw new DatabaseException("Failed to record donation, no rows inserted.");
                }

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedDonationId = keys.getInt(1);
                        donation.setDonationId(generatedDonationId);
                    } else {
                        throw new DatabaseException("Failed to obtain donation ID.");
                    }
                }
            }

            // If donation is COMPLETED, update the related request status and donor profile
            if (donation.getDonationStatus() == DonationStatus.COMPLETED) {
                // Update blood request status to COMPLETED
                String updateReqSql = "UPDATE blood_requests SET request_status = ?, updated_at = datetime('now', 'localtime') WHERE request_id = ?";
                try (PreparedStatement reqStmt = conn.prepareStatement(updateReqSql)) {
                    reqStmt.setString(1, RequestStatus.COMPLETED.name());
                    reqStmt.setInt(2, donation.getRequestId());
                    reqStmt.executeUpdate();
                }

                // Update donor profile last_donation_date
                String updateDonorSql = "UPDATE donor_profiles SET last_donation_date = COALESCE(?, datetime('now', 'localtime')), " +
                                        "updated_at = datetime('now', 'localtime') WHERE user_id = ?";
                try (PreparedStatement donorStmt = conn.prepareStatement(updateDonorSql)) {
                    donorStmt.setString(1, donation.getDonationDate());
                    donorStmt.setInt(2, donation.getDonorId());
                    donorStmt.executeUpdate();
                }
            } else if (donation.getDonationStatus() == DonationStatus.SCHEDULED) {
                // Mark request as ACCEPTED/MATCHED
                String updateReqSql = "UPDATE blood_requests SET request_status = ?, updated_at = datetime('now', 'localtime') WHERE request_id = ? AND request_status = 'PENDING'";
                try (PreparedStatement reqStmt = conn.prepareStatement(updateReqSql)) {
                    reqStmt.setString(1, RequestStatus.ACCEPTED.name());
                    reqStmt.setInt(2, donation.getRequestId());
                    reqStmt.executeUpdate();
                }
            }

            conn.commit(); // Commit Transaction
            return getDonationById(donation.getDonationId());
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException rollbackEx) {
                    System.err.println("Rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw new DatabaseException("Transaction failed while recording donation: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    // Ignore close exception
                }
            }
        }
    }

    /**
     * Retrieves a donation record by ID.
     */
    public Donation getDonationById(int donationId) {
        String sql = "SELECT d.donation_id, d.request_id, d.donor_id, d.donation_date, d.donation_status, d.notes, d.created_at, " +
                     "u.full_name AS donor_name, br.hospital_name, br.blood_group, ru.full_name AS recipient_name " +
                     "FROM donations d " +
                     "JOIN users u ON d.donor_id = u.user_id " +
                     "JOIN blood_requests br ON d.request_id = br.request_id " +
                     "JOIN users ru ON br.recipient_id = ru.user_id " +
                     "WHERE d.donation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDonation(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving donation by ID: " + donationId, e);
        }
    }

    /**
     * Retrieves all donations performed by a specific donor user ID.
     */
    public List<Donation> getDonationsByDonor(int donorId) {
        String sql = "SELECT d.donation_id, d.request_id, d.donor_id, d.donation_date, d.donation_status, d.notes, d.created_at, " +
                     "u.full_name AS donor_name, br.hospital_name, br.blood_group, ru.full_name AS recipient_name " +
                     "FROM donations d " +
                     "JOIN users u ON d.donor_id = u.user_id " +
                     "JOIN blood_requests br ON d.request_id = br.request_id " +
                     "JOIN users ru ON br.recipient_id = ru.user_id " +
                     "WHERE d.donor_id = ? " +
                     "ORDER BY d.donation_id DESC";
        List<Donation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donorId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDonation(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving donations for donor: " + donorId, e);
        }
    }

    /**
     * Retrieves all donations tied to a specific blood request ID.
     */
    public List<Donation> getDonationsByRequest(int requestId) {
        String sql = "SELECT d.donation_id, d.request_id, d.donor_id, d.donation_date, d.donation_status, d.notes, d.created_at, " +
                     "u.full_name AS donor_name, br.hospital_name, br.blood_group, ru.full_name AS recipient_name " +
                     "FROM donations d " +
                     "JOIN users u ON d.donor_id = u.user_id " +
                     "JOIN blood_requests br ON d.request_id = br.request_id " +
                     "JOIN users ru ON br.recipient_id = ru.user_id " +
                     "WHERE d.request_id = ? " +
                     "ORDER BY d.donation_id DESC";
        List<Donation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, requestId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDonation(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving donations for request: " + requestId, e);
        }
    }

    /**
     * Retrieves all donations in the system (for admin review).
     */
    public List<Donation> getAllDonations() {
        String sql = "SELECT d.donation_id, d.request_id, d.donor_id, d.donation_date, d.donation_status, d.notes, d.created_at, " +
                     "u.full_name AS donor_name, br.hospital_name, br.blood_group, ru.full_name AS recipient_name " +
                     "FROM donations d " +
                     "JOIN users u ON d.donor_id = u.user_id " +
                     "JOIN blood_requests br ON d.request_id = br.request_id " +
                     "JOIN users ru ON br.recipient_id = ru.user_id " +
                     "ORDER BY d.donation_id DESC";
        List<Donation> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToDonation(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all donations", e);
        }
    }

    /**
     * Updates the status of a donation.
     */
    public boolean updateDonationStatus(int donationId, DonationStatus status) {
        if (status == null) {
            throw new ValidationException("DonationStatus cannot be null");
        }

        String sql = "UPDATE donations SET donation_status = ? WHERE donation_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, donationId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating donation status for: " + donationId, e);
        }
    }

    private Donation mapRowToDonation(ResultSet rs) throws SQLException {
        Donation d = new Donation(
            rs.getInt("donation_id"),
            rs.getInt("request_id"),
            rs.getInt("donor_id"),
            rs.getString("donation_date"),
            DonationStatus.fromString(rs.getString("donation_status")),
            rs.getString("notes"),
            rs.getString("created_at")
        );
        d.setDonorName(rs.getString("donor_name"));
        d.setRecipientName(rs.getString("recipient_name"));
        d.setHospitalName(rs.getString("hospital_name"));
        d.setBloodGroup(BloodGroup.fromString(rs.getString("blood_group")));
        return d;
    }
}

