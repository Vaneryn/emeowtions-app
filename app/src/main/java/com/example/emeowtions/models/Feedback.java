package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class Feedback {

    private String uid;
    private String userDisplayName;
    private String userProfilePicture;
    private float rating;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Feedback() {}

    public Feedback(String uid, String userDisplayName, String userProfilePicture, float rating, String description, Timestamp createdAt, Timestamp updatedAt) {
        this.uid = uid;
        this.userDisplayName = userDisplayName;
        this.userProfilePicture = userProfilePicture;
        this.rating = rating;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUid() {
        return uid;
    }

    public String getUserDisplayName() {
        return userDisplayName;
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

    public void setUserProfilePicture(String userProfilePicture) {
        this.userProfilePicture = userProfilePicture;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
