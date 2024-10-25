package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class AnalysisFeedback {

    private String analysisId;
    private String uid;
    private String userEmail;
    private String userDisplayName;
    private String userPfpUrl;
    private float rating;
    private String description;
    private boolean read;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public AnalysisFeedback() {}

    public AnalysisFeedback(String analysisId, String uid, String userEmail, String userDisplayName, String userPfpUrl, float rating, String description, boolean read, Timestamp createdAt, Timestamp updatedAt) {
        this.analysisId = analysisId;
        this.uid = uid;
        this.userEmail = userEmail;
        this.userDisplayName = userDisplayName;
        this.userPfpUrl = userPfpUrl;
        this.rating = rating;
        this.description = description;
        this.read = read;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getAnalysisId() {
        return analysisId;
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

    public float getRating() {
        return rating;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRead() {
        return read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
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

    public void setRating(float rating) {
        this.rating = rating;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "AnalysisFeedback{" +
                "analysisId='" + analysisId + '\'' +
                ", uid='" + uid + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", userDisplayName='" + userDisplayName + '\'' +
                ", userPfpUrl='" + userPfpUrl + '\'' +
                ", rating=" + rating +
                ", description='" + description + '\'' +
                ", read=" + read +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
