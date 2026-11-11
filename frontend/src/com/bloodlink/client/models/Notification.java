package com.bloodlink.client.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Domain model representing a system or push notification.
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private String notificationId;
    private String title;
    private String message;
    private String urgency; // "INFO", "NORMAL", "URGENT", "CRITICAL"
    private boolean read;
    private long timestamp;

    public Notification() {
        this.timestamp = System.currentTimeMillis();
        this.urgency = "INFO";
        this.read = false;
    }

    public Notification(String notificationId, String title, String message, String urgency, boolean read, long timestamp) {
        this.notificationId = notificationId;
        this.title = title;
        this.message = message;
        this.urgency = urgency != null ? urgency.toUpperCase() : "INFO";
        this.read = read;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency != null ? urgency.toUpperCase() : "INFO";
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isUrgent() {
        return "URGENT".equalsIgnoreCase(urgency) || "CRITICAL".equalsIgnoreCase(urgency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification that)) return false;
        return Objects.equals(notificationId, that.notificationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId='" + notificationId + '\'' +
                ", title='" + title + '\'' +
                ", urgency='" + urgency + '\'' +
                ", read=" + read +
                '}';
    }
}
