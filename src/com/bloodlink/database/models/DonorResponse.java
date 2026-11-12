package com.bloodlink.database.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing a donor response (ACCEPTED, REJECTED, PENDING) to a blood request.
 */
public class DonorResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private int responseId;
    private int requestId;
    private int donorId;
    private int recipientId;
    private String status; // ACCEPTED, REJECTED, PENDING
    private String notes;
    private String createdAt;
    private String updatedAt;

    // Joined helper fields
    private String donorName;
    private String donorEmail;
    private String donorPhone;
    private String donorBloodGroup;
    private String donorLocation;
    private String donorAvailability;
    private String lastDonationDate;
    private String hospitalName;

    public DonorResponse() {
        this.status = "PENDING";
    }

    public DonorResponse(int requestId, int donorId, int recipientId, String status, String notes) {
        this.requestId = requestId;
        this.donorId = donorId;
        this.recipientId = recipientId;
        this.status = status;
        this.notes = notes;
    }

    public DonorResponse(int responseId, int requestId, int donorId, int recipientId,
                         String status, String notes, String createdAt, String updatedAt) {
        this.responseId = responseId;
        this.requestId = requestId;
        this.donorId = donorId;
        this.recipientId = recipientId;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getResponseId() {
        return responseId;
    }

    public void setResponseId(int responseId) {
        this.responseId = responseId;
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

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getDonorBloodGroup() {
        return donorBloodGroup;
    }

    public void setDonorBloodGroup(String donorBloodGroup) {
        this.donorBloodGroup = donorBloodGroup;
    }

    public String getDonorLocation() {
        return donorLocation;
    }

    public void setDonorLocation(String donorLocation) {
        this.donorLocation = donorLocation;
    }

    public String getDonorAvailability() {
        return donorAvailability;
    }

    public void setDonorAvailability(String donorAvailability) {
        this.donorAvailability = donorAvailability;
    }

    public String getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(String lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DonorResponse that = (DonorResponse) o;
        return responseId == that.responseId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseId);
    }

    @Override
    public String toString() {
        return "DonorResponse{" +
                "responseId=" + responseId +
                ", requestId=" + requestId +
                ", donorId=" + donorId +
                ", recipientId=" + recipientId +
                ", status='" + status + '\'' +
                ", donorName='" + donorName + '\'' +
                ", donorBloodGroup='" + donorBloodGroup + '\'' +
                '}';
    }
}

