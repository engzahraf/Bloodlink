package com.bloodlink.database.models;

import com.bloodlink.database.enums.AvailabilityStatus;
import com.bloodlink.database.enums.BloodGroup;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing detailed donor profile information.
 */
public class DonorProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private int donorId;
    private int userId;
    private BloodGroup bloodGroup;
    private String location;
    private AvailabilityStatus availabilityStatus;
    private String lastDonationDate;
    private String createdAt;
    private String updatedAt;

    // Additional fields for convenient joins
    private String donorName;
    private String donorEmail;
    private String donorPhone;

    public DonorProfile() {
        this.availabilityStatus = AvailabilityStatus.AVAILABLE;
    }

    public DonorProfile(int userId, BloodGroup bloodGroup, String location, AvailabilityStatus availabilityStatus) {
        this.userId = userId;
        this.bloodGroup = bloodGroup;
        this.location = location;
        this.availabilityStatus = availabilityStatus;
    }

    public DonorProfile(int donorId, int userId, BloodGroup bloodGroup, String location,
                        AvailabilityStatus availabilityStatus, String lastDonationDate,
                        String createdAt, String updatedAt) {
        this.donorId = donorId;
        this.userId = userId;
        this.bloodGroup = bloodGroup;
        this.location = location;
        this.availabilityStatus = availabilityStatus;
        this.lastDonationDate = lastDonationDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(String lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorEmail() {
        return donorEmail;
    }

    public void setDonorEmail(String donorEmail) {
        this.donorEmail = donorEmail;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public boolean isAvailable() {
        return this.availabilityStatus == AvailabilityStatus.AVAILABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DonorProfile that = (DonorProfile) o;
        return donorId == that.donorId && userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(donorId, userId);
    }

    @Override
    public String toString() {
        return "DonorProfile{" +
                "donorId=" + donorId +
                ", userId=" + userId +
                ", bloodGroup=" + bloodGroup +
                ", location='" + location + '\'' +
                ", availabilityStatus=" + availabilityStatus +
                ", lastDonationDate='" + lastDonationDate + '\'' +
                '}';
    }
}

