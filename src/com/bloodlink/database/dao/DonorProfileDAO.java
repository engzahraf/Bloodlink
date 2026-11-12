package com.bloodlink.database.dao;

import com.bloodlink.database.DatabaseConnection;
import com.bloodlink.database.enums.AvailabilityStatus;
import com.bloodlink.database.enums.BloodGroup;
import com.bloodlink.database.exceptions.DatabaseException;
import com.bloodlink.database.exceptions.ValidationException;
import com.bloodlink.database.models.DonorProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Access Object for DonorProfile entities.
 */
public class DonorProfileDAO {

    /**
     * Creates a new donor profile linked to an existing user.
     */
    public DonorProfile createDonorProfile(DonorProfile profile) {
        if (profile == null) {
            throw new ValidationException("DonorProfile cannot be null");
        }
        if (profile.getUserId() <= 0) {
            throw new ValidationException("Valid userId is required");
        }
        if (profile.getBloodGroup() == null) {
            throw new ValidationException("Blood group is required");
        }
        if (profile.getLocation() == null || profile.getLocation().trim().isEmpty()) {
            throw new ValidationException("Location is required");
        }

        String sql = "INSERT INTO donor_profiles (user_id, blood_group, location, availability_status, last_donation_date) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, profile.getUserId());
            stmt.setString(2, profile.getBloodGroup().name());
            stmt.setString(3, profile.getLocation().trim());
            stmt.setString(4, profile.getAvailabilityStatus() != null ? profile.getAvailabilityStatus().name() : AvailabilityStatus.AVAILABLE.name());
            stmt.setString(5, profile.getLastDonationDate());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating donor profile failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    profile.setDonorId(generatedKeys.getInt(1));
                } else {
                    throw new DatabaseException("Creating donor profile failed, no ID obtained.");
                }
            }

            return getDonorById(profile.getDonorId());
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed: donor_profiles.user_id")) {
                throw new ValidationException("A donor profile already exists for user ID: " + profile.getUserId());
            }
            throw new DatabaseException("Error creating donor profile: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves a donor profile by the associated user ID.
     */
    public DonorProfile getDonorByUserId(int userId) {
        String sql = "SELECT dp.donor_id, dp.user_id, dp.blood_group, dp.location, dp.availability_status, " +
                     "dp.last_donation_date, dp.created_at, dp.updated_at, " +
                     "u.full_name, u.email, u.phone " +
                     "FROM donor_profiles dp " +
                     "JOIN users u ON dp.user_id = u.user_id " +
                     "WHERE dp.user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDonorProfile(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving donor profile for userId: " + userId, e);
        }
    }

    /**
     * Retrieves a donor profile by its primary key ID.
     */
    public DonorProfile getDonorById(int donorId) {
        String sql = "SELECT dp.donor_id, dp.user_id, dp.blood_group, dp.location, dp.availability_status, " +
                     "dp.last_donation_date, dp.created_at, dp.updated_at, " +
                     "u.full_name, u.email, u.phone " +
                     "FROM donor_profiles dp " +
                     "JOIN users u ON dp.user_id = u.user_id " +
                     "WHERE dp.donor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donorId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToDonorProfile(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving donor profile by ID: " + donorId, e);
        }
    }

    /**
     * Retrieves all donor profiles.
     */
    public List<DonorProfile> getAllDonors() {
        String sql = "SELECT dp.donor_id, dp.user_id, dp.blood_group, dp.location, dp.availability_status, " +
                     "dp.last_donation_date, dp.created_at, dp.updated_at, " +
                     "u.full_name, u.email, u.phone " +
                     "FROM donor_profiles dp " +
                     "JOIN users u ON dp.user_id = u.user_id " +
                     "ORDER BY dp.donor_id ASC";
        List<DonorProfile> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToDonorProfile(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving all donors", e);
        }
    }

    /**
     * Searches for active donors with an exact blood group.
     */
    public List<DonorProfile> searchDonorsByBloodGroup(BloodGroup bloodGroup) {
        if (bloodGroup == null) return getAllDonors();

        String sql = "SELECT dp.donor_id, dp.user_id, dp.blood_group, dp.location, dp.availability_status, " +
                     "dp.last_donation_date, dp.created_at, dp.updated_at, " +
                     "u.full_name, u.email, u.phone " +
                     "FROM donor_profiles dp " +
                     "JOIN users u ON dp.user_id = u.user_id " +
                     "WHERE dp.blood_group = ? AND u.account_status = 'ACTIVE' " +
                     "ORDER BY dp.donor_id ASC";
        List<DonorProfile> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, bloodGroup.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDonorProfile(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching donors by blood group: " + bloodGroup, e);
        }
    }

    /**
     * Searches for donors in a specified location (case-insensitive substring match).
     */
    public List<DonorProfile> searchDonorsByLocation(String location) {
        if (location == null || location.trim().isEmpty()) return getAllDonors();

        String pattern = "%" + location.trim().toLowerCase() + "%";
        String sql = "SELECT dp.donor_id, dp.user_id, dp.blood_group, dp.location, dp.availability_status, " +
                     "dp.last_donation_date, dp.created_at, dp.updated_at, " +
                     "u.full_name, u.email, u.phone " +
                     "FROM donor_profiles dp " +
                     "JOIN users u ON dp.user_id = u.user_id " +
                     "WHERE LOWER(dp.location) LIKE ? AND u.account_status = 'ACTIVE' " +
                     "ORDER BY dp.donor_id ASC";
        List<DonorProfile> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDonorProfile(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching donors by location: " + location, e);
        }
    }

    /**
     * Searches for all donors currently marked as AVAILABLE and with ACTIVE accounts.
     */
    public List<DonorProfile> searchAvailableDonors() {
        String sql = "SELECT dp.donor_id, dp.user_id, dp.blood_group, dp.location, dp.availability_status, " +
                     "dp.last_donation_date, dp.created_at, dp.updated_at, " +
                     "u.full_name, u.email, u.phone " +
                     "FROM donor_profiles dp " +
                     "JOIN users u ON dp.user_id = u.user_id " +
                     "WHERE dp.availability_status = 'AVAILABLE' AND u.account_status = 'ACTIVE' " +
                     "ORDER BY dp.donor_id ASC";
        List<DonorProfile> list = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToDonorProfile(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching available donors", e);
        }
    }

    /**
     * High-level matching search: returns available donors whose blood group is medically
     * compatible with the recipient, optionally filtered by location.
     */
    public List<DonorProfile> searchCompatibleDonors(BloodGroup recipientBloodGroup, String locationFilter) {
        if (recipientBloodGroup == null) {
            return searchAvailableDonors();
        }

        List<BloodGroup> compatibleGroups = recipientBloodGroup.getCompatibleDonorGroups();
        if (compatibleGroups.isEmpty()) {
            return new ArrayList<>();
        }

        String inClause = compatibleGroups.stream()
                .map(bg -> "'" + bg.name() + "'")
                .collect(Collectors.joining(", "));

        StringBuilder sql = new StringBuilder(
            "SELECT dp.donor_id, dp.user_id, dp.blood_group, dp.location, dp.availability_status, " +
            "dp.last_donation_date, dp.created_at, dp.updated_at, " +
            "u.full_name, u.email, u.phone " +
            "FROM donor_profiles dp " +
            "JOIN users u ON dp.user_id = u.user_id " +
            "WHERE dp.availability_status = 'AVAILABLE' AND u.account_status = 'ACTIVE' " +
            "AND dp.blood_group IN (" + inClause + ") "
        );

        boolean hasLocation = locationFilter != null && !locationFilter.trim().isEmpty();
        if (hasLocation) {
            sql.append("AND LOWER(dp.location) LIKE ? ");
        }
        sql.append("ORDER BY dp.donor_id ASC");

        List<DonorProfile> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if (hasLocation) {
                stmt.setString(1, "%" + locationFilter.trim().toLowerCase() + "%");
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToDonorProfile(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching compatible donors", e);
        }
    }

    /**
     * Updates donor location and blood group.
     */
    public boolean updateDonorProfile(DonorProfile profile) {
        if (profile == null || profile.getDonorId() <= 0) {
            throw new ValidationException("Valid donor profile with ID is required");
        }

        String sql = "UPDATE donor_profiles SET blood_group = ?, location = ?, " +
                     "updated_at = datetime('now', 'localtime') WHERE donor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, profile.getBloodGroup().name());
            stmt.setString(2, profile.getLocation().trim());
            stmt.setInt(3, profile.getDonorId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating donor profile: " + profile.getDonorId(), e);
        }
    }

    /**
     * Updates donor availability status (AVAILABLE or UNAVAILABLE).
     */
    public boolean updateAvailability(int donorId, AvailabilityStatus status) {
        if (status == null) {
            throw new ValidationException("AvailabilityStatus cannot be null");
        }

        String sql = "UPDATE donor_profiles SET availability_status = ?, " +
                     "updated_at = datetime('now', 'localtime') WHERE donor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, donorId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating availability for donor: " + donorId, e);
        }
    }

    /**
     * Updates the last donation date of a donor.
     */
    public boolean updateLastDonationDate(int donorId, String donationDate) {
        String sql = "UPDATE donor_profiles SET last_donation_date = ?, " +
                     "updated_at = datetime('now', 'localtime') WHERE donor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, donationDate);
            stmt.setInt(2, donorId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating last donation date for donor: " + donorId, e);
        }
    }

    /**
     * Deletes a donor profile by its ID.
     */
    public boolean deleteDonorProfile(int donorId) {
        String sql = "DELETE FROM donor_profiles WHERE donor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, donorId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting donor profile: " + donorId, e);
        }
    }

    private DonorProfile mapRowToDonorProfile(ResultSet rs) throws SQLException {
        DonorProfile profile = new DonorProfile(
            rs.getInt("donor_id"),
            rs.getInt("user_id"),
            BloodGroup.fromString(rs.getString("blood_group")),
            rs.getString("location"),
            AvailabilityStatus.fromString(rs.getString("availability_status")),
            rs.getString("last_donation_date"),
            rs.getString("created_at"),
            rs.getString("updated_at")
        );
        profile.setDonorName(rs.getString("full_name"));
        profile.setDonorEmail(rs.getString("email"));
        profile.setDonorPhone(rs.getString("phone"));
        return profile;
    }
}

