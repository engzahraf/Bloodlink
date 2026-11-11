package com.bloodlink.database.models;

import com.bloodlink.database.enums.BloodGroup;
import com.bloodlink.database.enums.DonationStatus;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing a blood donation record.
 */
public class Donation implements Serializable {
    private static final long serialVersionUID = 1L;

    private int donationId;
    private int requestId;
    private int donorId;
    private String donationDate;
    private DonationStatus donationStatus;
    private String notes;
    private String createdAt;

    // Joined helper fields
    private String donorName;
    private String recipientName;
    private String hospitalName;
    private BloodGroup bloodGroup;

    public Donation() {
        this.donationStatus = DonationStatus.SCHEDULED;
    }

    public Donation(int requestId, int donorId, String donationDate, DonationStatus donationStatus, String notes) {
        this.requestId = requestId;
        this.donorId = donorId;
        this.donationDate = donationDate;
        this.donationStatus = donationStatus;
        this.notes = notes;
    }

    public Donation(int donationId, int requestId, int donorId, String donationDate,
                    DonationStatus donationStatus, String notes, String createdAt) {
        this.donationId = donationId;
        this.requestId = requestId;
        this.donorId = donorId;
        this.donationDate = donationDate;
        this.donationStatus = donationStatus;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public int getDonationId() {
        return donationId;
    }

    public void setDonationId(int donationId) {
        this.donationId = donationId;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getDonorId() {
        return donorId;
    }

    public void setDonorId(int donorId) {
        this.donorId = donorId;
    }

    public String getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(String donationDate) {
        this.donationDate = donationDate;
    }

    public DonationStatus getDonationStatus() {
        return donationStatus;
    }

    public void setDonationStatus(DonationStatus donationStatus) {
        this.donationStatus = donationStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Donation donation = (Donation) o;
        return donationId == donation.donationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(donationId);
    }

    @Override
    public String toString() {
        return "Donation{" +
                "donationId=" + donationId +
                ", requestId=" + requestId +
                ", donorId=" + donorId +
                ", donationDate='" + donationDate + '\'' +
                ", donationStatus=" + donationStatus +
                ", notes='" + notes + '\'' +
                '}';
    }
}

