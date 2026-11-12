package com.bloodlink.client.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Domain model representing a blood donor's profile.
 */
public class Donor implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;
    private String fullName;
    private String bloodGroup;
    private String phone;
    private String email;
    private String city;
    private boolean available;
    private String lastDonationDate;
    private int totalDonations;

    public Donor() {
        this.available = true;
    }

    public Donor(String userId, String fullName, String bloodGroup, String phone, String email, String city, boolean available, String lastDonationDate, int totalDonations) {
        this.userId = userId;
        this.fullName = fullName;
        this.bloodGroup = bloodGroup;
        this.phone = phone;
        this.email = email;
        this.city = city;
        this.available = available;
        this.lastDonationDate = lastDonationDate;
        this.totalDonations = totalDonations;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(String lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public int getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(int totalDonations) {
        this.totalDonations = totalDonations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Donor donor)) return false;
        return Objects.equals(userId, donor.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "Donor{" +
                "userId='" + userId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", bloodGroup='" + bloodGroup + '\'' +
                ", city='" + city + '\'' +
                ", available=" + available +
                '}';
    }
}
