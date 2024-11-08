package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class ChatRequest {
    private String veterinaryClinicId;
    private String uid;
    private String userEmail;
    private String userDisplayName;
    private String userPfpUrl;
    private String description;
    private boolean accepted;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ChatRequest() {}

    public ChatRequest(String veterinaryClinicId, String uid, String userEmail, String userDisplayName, String userPfpUrl, String description, boolean accepted, Timestamp createdAt, Timestamp updatedAt) {
        this.veterinaryClinicId = veterinaryClinicId;
        this.uid = uid;
        this.userEmail = userEmail;
        this.userDisplayName = userDisplayName;
        this.userPfpUrl = userPfpUrl;
        this.description = description;
        this.accepted = accepted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getVeterinaryClinicId() {
        return veterinaryClinicId;
    }

    public String getUid() {
        return uid;
    }

    public String getUserEmail() {
        return userEmail;
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

    public void setVeterinaryClinicId(String veterinaryClinicId) {
        this.veterinaryClinicId = veterinaryClinicId;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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
                "veterinaryClinicId='" + veterinaryClinicId + '\'' +
                ", uid='" + uid + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", userDisplayName='" + userDisplayName + '\'' +
                ", userPfpUrl='" + userPfpUrl + '\'' +
                ", description='" + description + '\'' +
                ", accepted=" + accepted +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
