package com.example.emeowtions.models;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.util.Date;

public class User {

    private String displayName;
    private String email;
    private String gender;
    private String role;
    private String profilePicture;
    private Date dateOfBirth;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public User() {}

    public User(String displayName, String email, String gender, String role, String profilePicture, Date dateOfBirth, Timestamp createdAt, Timestamp updatedAt) {
        this.displayName = displayName;
        this.email = email;
        this.gender = gender;
        this.role = role;
        this.profilePicture = profilePicture;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getGender() {
        return gender;
    }

    public String getRole() {
        return role;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                ", displayName='" + displayName + '\'' +
                ", email='" + email + '\'' +
                ", gender='" + gender + '\'' +
                ", role='" + role + '\'' +
                ", profilePicture='" + profilePicture + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
