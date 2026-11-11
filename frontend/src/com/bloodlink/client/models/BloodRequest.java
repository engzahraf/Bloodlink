package com.bloodlink.client.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Domain model representing a blood donation request.
 */
public class BloodRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String recipientId;
    private String recipientName;
    private String bloodGroup;
    private int unitsRequired;
    private String hospital;
    private String city;
    private String urgency; // "NORMAL", "URGENT", "CRITICAL"
    private String status;  // "OPEN", "IN_PROGRESS", "FULFILLED", "CANCELLED"
    private String notes;
    private long createdAt;

    public BloodRequest() {
        this.unitsRequired = 1;
        this.urgency = "NORMAL";
        this.status = "OPEN";
        this.createdAt = System.currentTimeMillis();
    }

    public BloodRequest(String requestId, String recipientId, String recipientName, String bloodGroup, int unitsRequired, String hospital, String city, String urgency, String status, String notes, long createdAt) {
        this.requestId = requestId;
        this.recipientId = recipientId;
        this.recipientName = recipientName;
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.hospital = hospital;
        this.city = city;
        this.urgency = urgency != null ? urgency.toUpperCase() : "NORMAL";
        this.status = status != null ? status.toUpperCase() : "OPEN";
        this.notes = notes;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public int getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(int unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency != null ? urgency.toUpperCase() : "NORMAL";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status.toUpperCase() : "OPEN";
    }

    public String getNotes() {
        return notes;
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

    public boolean isUrgent() {
        return "URGENT".equalsIgnoreCase(urgency) || "CRITICAL".equalsIgnoreCase(urgency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BloodRequest that)) return false;
        return Objects.equals(requestId, that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "BloodRequest{" +
                "requestId='" + requestId + '\'' +
                ", bloodGroup='" + bloodGroup + '\'' +
                ", hospital='" + hospital + '\'' +
                ", urgency='" + urgency + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
