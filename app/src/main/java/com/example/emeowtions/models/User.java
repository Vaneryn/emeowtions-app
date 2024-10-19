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
    private Timestamp dateOfBirth;
    private boolean verified;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean deleted;

    public User() {}

    public User(String displayName, String email, String gender, String role, String profilePicture, Timestamp dateOfBirth, boolean verified, Timestamp createdAt, Timestamp updatedAt, boolean deleted) {
        this.displayName = displayName;
        this.email = email;
        this.gender = gender;
        this.role = role;
        this.profilePicture = profilePicture;
        this.dateOfBirth = dateOfBirth;
        this.verified = verified;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
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

    public Timestamp getDateOfBirth() {
        return dateOfBirth;
    }

    public boolean isVerified() {
        return verified;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
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

    public void setDateOfBirth(Timestamp dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
