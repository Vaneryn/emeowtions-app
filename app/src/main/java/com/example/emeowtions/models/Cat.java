package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class Cat {

    private String name;
    private String gender;
    private String breed;
    private Timestamp dateOfBirth;
    private String background;
    private String medicalConditions;
    private String description;
    private String profilePicture;
    private String ownerId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean deleted;

    public Cat() {}

    public Cat(String name, String gender, String breed, Timestamp dateOfBirth, String background, String medicalConditions, String description, String profilePicture, String ownerId, Timestamp createdAt, Timestamp updatedAt, boolean deleted) {
        this.name = name;
        this.gender = gender;
        this.breed = breed;
        this.dateOfBirth = dateOfBirth;
        this.background = background;
        this.medicalConditions = medicalConditions;
        this.description = description;
        this.profilePicture = profilePicture;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getBreed() {
        return breed;
    }

    public Timestamp getDateOfBirth() {
        return dateOfBirth;
    }

    public String getBackground() {
        return background;
    }

    public String getMedicalConditions() {
        return medicalConditions;
    }

    public String getDescription() {
        return description;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public String getOwnerId() {
        return ownerId;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public void setDateOfBirth(Timestamp dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public void setMedicalConditions(String medicalConditions) {
        this.medicalConditions = medicalConditions;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
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
