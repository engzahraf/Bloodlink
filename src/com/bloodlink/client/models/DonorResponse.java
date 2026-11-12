package com.bloodlink.client.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Domain model representing a donor response on the client frontend.
 */
public class DonorResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String responseId;
    private String requestId;
    private String donorId;
    private String recipientId;
    private String donorName;
    private String donorBloodGroup;
    private String donorPhone;
    private String donorEmail;
    private String donorCity;
    private String donorAvailability;
    private String lastDonationDate;
    private String status; // ACCEPTED, REJECTED, PENDING
    private String notes;
    private long createdAt;

    public DonorResponse() {
        this.status = "PENDING";
    }

    public DonorResponse(String responseId, String requestId, String donorId, String recipientId,
                         String donorName, String donorBloodGroup, String donorPhone, String donorEmail,
                         String donorCity, String donorAvailability, String lastDonationDate,
                         String status, String notes, long createdAt) {
        this.responseId = responseId;
        this.requestId = requestId;
        this.donorId = donorId;
        this.recipientId = recipientId;
        this.donorName = donorName;
        this.donorBloodGroup = donorBloodGroup;
        this.donorPhone = donorPhone;
        this.donorEmail = donorEmail;
        this.donorCity = donorCity;
        this.donorAvailability = donorAvailability;
        this.lastDonationDate = lastDonationDate;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDonorId() {
        return donorId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getDonorName() {
        return donorName != null && !donorName.isEmpty() ? donorName : "Donor " + donorId;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorBloodGroup() {
        return donorBloodGroup != null ? donorBloodGroup : "";
    }

    public void setDonorBloodGroup(String donorBloodGroup) {
        this.donorBloodGroup = donorBloodGroup;
    }

    public String getDonorPhone() {
        return donorPhone != null ? donorPhone : "";
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public String getDonorEmail() {
        return donorEmail != null ? donorEmail : "";
    }

    public void setDonorEmail(String donorEmail) {
        this.donorEmail = donorEmail;
    }

    public String getDonorCity() {
        return donorCity != null ? donorCity : "";
    }

    public void setDonorCity(String donorCity) {
        this.donorCity = donorCity;
    }

    public String getDonorAvailability() {
        return donorAvailability != null ? donorAvailability : "AVAILABLE";
    }

    public void setDonorAvailability(String donorAvailability) {
        this.donorAvailability = donorAvailability;
    }

    public String getLastDonationDate() {
        return lastDonationDate != null ? lastDonationDate : "Never";
    }

    public void setLastDonationDate(String lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public String getStatus() {
        return status != null ? status.toUpperCase() : "PENDING";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes != null ? notes : "";
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAccepted() {
        return "ACCEPTED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DonorResponse that = (DonorResponse) o;
        return Objects.equals(responseId, that.responseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responseId);
    }

    @Override
    public String toString() {
        return "DonorResponse{" +
                "responseId='" + responseId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", donorName='" + donorName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

