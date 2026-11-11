package com.bloodlink.database.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model representing direct communication between users.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private int messageId;
    private int senderId;
    private int receiverId;
    private Integer requestId; // nullable
    private String messageContent;
    private String sentAt;
    private boolean read;

    // Joined helper fields
    private String senderName;
    private String receiverName;

    public Message() {
        this.read = false;
    }

    public Message(int senderId, int receiverId, Integer requestId, String messageContent) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.requestId = requestId;
        this.messageContent = messageContent;
        this.read = false;
    }

    public Message(int messageId, int senderId, int receiverId, Integer requestId,
                   String messageContent, String sentAt, boolean read) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.requestId = requestId;
        this.messageContent = messageContent;
        this.sentAt = sentAt;
        this.read = read;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return messageId == message.messageId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }

    @Override
    public String toString() {
        return "Message{" +
                "messageId=" + messageId +
                ", senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", requestId=" + requestId +
                ", sentAt='" + sentAt + '\'' +
                ", read=" + read +
                '}';
    }
}

