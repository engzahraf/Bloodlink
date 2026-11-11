package com.bloodlink.database.models;

import com.bloodlink.database.enums.BloodGroup;
import com.bloodlink.database.enums.RequestStatus;
import com.bloodlink.database.enums.UrgencyLevel;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing a recipient blood request.
 */
public class BloodRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int requestId;
    private int recipientId;
    private BloodGroup bloodGroup;
    private int unitsRequired;
    private String hospitalName;
    private String location;
    private String contactNumber;
    private UrgencyLevel urgencyLevel;
    private RequestStatus requestStatus;
    private String description;
    private String createdAt;
    private String updatedAt;

    // Convenience fields
    private String recipientName;
    private String recipientEmail;

    public BloodRequest() {
        this.requestStatus = RequestStatus.PENDING;
        this.urgencyLevel = UrgencyLevel.MEDIUM;
    }

    public BloodRequest(int recipientId, BloodGroup bloodGroup, int unitsRequired,
                        String hospitalName, String location, String contactNumber,
                        UrgencyLevel urgencyLevel, String description) {
        this.recipientId = recipientId;
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.hospitalName = hospitalName;
        this.location = location;
        this.contactNumber = contactNumber;
        this.urgencyLevel = urgencyLevel;
        this.requestStatus = RequestStatus.PENDING;
        this.description = description;
    }

    public BloodRequest(int requestId, int recipientId, BloodGroup bloodGroup, int unitsRequired,
                        String hospitalName, String location, String contactNumber,
                        UrgencyLevel urgencyLevel, RequestStatus requestStatus,
                        String description, String createdAt, String updatedAt) {
        this.requestId = requestId;
        this.recipientId = recipientId;
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
        this.hospitalName = hospitalName;
        this.location = location;
        this.contactNumber = contactNumber;
        this.urgencyLevel = urgencyLevel;
        this.requestStatus = requestStatus;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public int getUnitsRequired() {
        return unitsRequired;
    }

    public void setUnitsRequired(int unitsRequired) {
        this.unitsRequired = unitsRequired;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BloodRequest that = (BloodRequest) o;
        return requestId == that.requestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "BloodRequest{" +
                "requestId=" + requestId +
                ", recipientId=" + recipientId +
                ", bloodGroup=" + bloodGroup +
                ", unitsRequired=" + unitsRequired +
                ", hospitalName='" + hospitalName + '\'' +
                ", location='" + location + '\'' +
                ", urgencyLevel=" + urgencyLevel +
                ", requestStatus=" + requestStatus +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}

