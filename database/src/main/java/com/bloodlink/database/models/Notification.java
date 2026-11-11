package com.bloodlink.database.models;

import com.bloodlink.database.enums.NotificationType;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing a user notification.
 */
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private int notificationId;
    private int userId;
    private NotificationType notificationType;
    private String message;
    private Integer relatedRequestId; // nullable
    private boolean read;
    private String createdAt;

    public Notification() {
        this.read = false;
    }

    public Notification(int userId, NotificationType notificationType, String message, Integer relatedRequestId) {
        this.userId = userId;
        this.notificationType = notificationType;
        this.message = message;
        this.relatedRequestId = relatedRequestId;
        this.read = false;
    }

    public Notification(int notificationId, int userId, NotificationType notificationType,
                        String message, Integer relatedRequestId, boolean read, String createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.message = message;
        this.relatedRequestId = relatedRequestId;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRelatedRequestId() {
        return relatedRequestId;
    }

    public void setRelatedRequestId(Integer relatedRequestId) {
        this.relatedRequestId = relatedRequestId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return notificationId == that.notificationId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", userId=" + userId +
                ", notificationType=" + notificationType +
                ", message='" + message + '\'' +
                ", relatedRequestId=" + relatedRequestId +
                ", read=" + read +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}

