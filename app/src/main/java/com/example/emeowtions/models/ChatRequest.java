package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class ChatRequest {
    private String uid;
    private String userDisplayName;
    private String userPfpUrl;
    private String description;
    private boolean accepted;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ChatRequest() {}

    public ChatRequest(String uid, String userDisplayName, String userPfpUrl, String description, boolean accepted, Timestamp createdAt, Timestamp updatedAt) {
        this.uid = uid;
        this.userDisplayName = userDisplayName;
        this.userPfpUrl = userPfpUrl;
        this.description = description;
        this.accepted = accepted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public String getUserPfpUrl() {
        return userPfpUrl;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public void setUserPfpUrl(String userPfpUrl) {
        this.userPfpUrl = userPfpUrl;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ChatRequest{" +
                "uid='" + uid + '\'' +
                ", userDisplayName='" + userDisplayName + '\'' +
                ", userPfpUrl='" + userPfpUrl + '\'' +
                ", description='" + description + '\'' +
                ", accepted=" + accepted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
