package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class Feedback {

    private String uid;
    private String userDisplayName;
    private String userEmail;
    private String userProfilePicture;
    private float rating;
    private String description;
    private boolean read;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Feedback() {}

    public Feedback(String uid, String userDisplayName, String userEmail, String userProfilePicture, float rating, String description, boolean read, Timestamp createdAt, Timestamp updatedAt) {
        this.uid = uid;
        this.userDisplayName = userDisplayName;
        this.userEmail = userEmail;
        this.userProfilePicture = userProfilePicture;
        this.rating = rating;
        this.description = description;
        this.read = read;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getUserProfilePicture() {
        return userProfilePicture;
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

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setUserProfilePicture(String userProfilePicture) {
        this.userProfilePicture = userProfilePicture;
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
        return "Feedback{" +
                "uid='" + uid + '\'' +
                ", userDisplayName='" + userDisplayName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", userProfilePicture='" + userProfilePicture + '\'' +
                ", rating=" + rating +
                ", description='" + description + '\'' +
                ", read=" + read +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
