package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Chat {

    private String chatRequestId;
    private String vetId;
    private String vetDisplayName;
    private String vetPfpUrl;
    private int vetUnreadCount;
    private boolean readByVet;
    private String userId;
    private String userDisplayName;
    private String userPfpUrl;
    private int userUnreadCount;
    private boolean readByUser;
    private String latestMessageText;
    private String latestMessageSenderUid;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Chat() {}

    public Chat(String chatRequestId, String vetId, String vetDisplayName, String vetPfpUrl, int vetUnreadCount, boolean readByVet, String userId, String userDisplayName, String userPfpUrl, int userUnreadCount, boolean readByUser, String latestMessageText, String latestMessageSenderUid, Timestamp createdAt, Timestamp updatedAt) {
        this.chatRequestId = chatRequestId;
        this.vetId = vetId;
        this.vetDisplayName = vetDisplayName;
        this.vetPfpUrl = vetPfpUrl;
        this.vetUnreadCount = vetUnreadCount;
        this.readByVet = readByVet;
        this.userId = userId;
        this.userDisplayName = userDisplayName;
        this.userPfpUrl = userPfpUrl;
        this.userUnreadCount = userUnreadCount;
        this.readByUser = readByUser;
        this.latestMessageText = latestMessageText;
        this.latestMessageSenderUid = latestMessageSenderUid;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getChatRequestId() {
        return chatRequestId;
    }

    public String getVetId() {
        return vetId;
    }

    public String getVetDisplayName() {
        return vetDisplayName;
    }

    public String getVetPfpUrl() {
        return vetPfpUrl;
    }

    public int getVetUnreadCount() {
        return vetUnreadCount;
    }

    public boolean isReadByVet() {
        return readByVet;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public String getUserPfpUrl() {
        return userPfpUrl;
    }

    public int getUserUnreadCount() {
        return userUnreadCount;
    }

    public boolean isReadByUser() {
        return readByUser;
    }

    public String getLatestMessageText() {
        return latestMessageText;
    }

    public String getLatestMessageSenderUid() {
        return latestMessageSenderUid;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setChatRequestId(String chatRequestId) {
        this.chatRequestId = chatRequestId;
    }

    public void setVetId(String vetId) {
        this.vetId = vetId;
    }

    public void setVetDisplayName(String vetDisplayName) {
        this.vetDisplayName = vetDisplayName;
    }

    public void setVetPfpUrl(String vetPfpUrl) {
        this.vetPfpUrl = vetPfpUrl;
    }

    public void setVetUnreadCount(int vetUnreadCount) {
        this.vetUnreadCount = vetUnreadCount;
    }

    public void setReadByVet(boolean readByVet) {
        this.readByVet = readByVet;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public void setUserPfpUrl(String userPfpUrl) {
        this.userPfpUrl = userPfpUrl;
    }

    public void setUserUnreadCount(int userUnreadCount) {
        this.userUnreadCount = userUnreadCount;
    }

    public void setReadByUser(boolean readByUser) {
        this.readByUser = readByUser;
    }

    public void setLatestMessageText(String latestMessageText) {
        this.latestMessageText = latestMessageText;
    }

    public void setLatestMessageSenderUid(String latestMessageSenderUid) {
        this.latestMessageSenderUid = latestMessageSenderUid;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "chatRequestId='" + chatRequestId + '\'' +
                ", vetId='" + vetId + '\'' +
                ", vetDisplayName='" + vetDisplayName + '\'' +
                ", vetPfpUrl='" + vetPfpUrl + '\'' +
                ", vetUnreadCount=" + vetUnreadCount +
                ", readByVet=" + readByVet +
                ", userId='" + userId + '\'' +
                ", userDisplayName='" + userDisplayName + '\'' +
                ", userPfpUrl='" + userPfpUrl + '\'' +
                ", userUnreadCount=" + userUnreadCount +
                ", readByUser=" + readByUser +
                ", latestMessageText='" + latestMessageText + '\'' +
                ", latestMessageSenderUid='" + latestMessageSenderUid + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
