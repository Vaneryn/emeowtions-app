package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class VeterinaryClinic {
    private String name;
    private String description;
    private String address;
    private String email;
    private String phoneNumber;
    private String logoUrl;
    private int reviewCount;
    private float totalRating;
    private float averageRating;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean deleted;

    private VeterinaryClinic() {}

    public VeterinaryClinic(String name, String description, String address, String email, String phoneNumber, String logoUrl, int reviewCount, float totalRating, float averageRating, Timestamp createdAt, Timestamp updatedAt, boolean deleted) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.logoUrl = logoUrl;
        this.reviewCount = reviewCount;
        this.totalRating = totalRating;
        this.averageRating = averageRating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public float getTotalRating() {
        return totalRating;
    }

    public float getAverageRating() {
        return averageRating;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public void setTotalRating(float totalRating) {
        this.totalRating = totalRating;
    }

    public void setAverageRating(float averageRating) {
        this.averageRating = averageRating;
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

    @Override
    public String toString() {
        return "VeterinaryClinic{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", address='" + address + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", logoUrl='" + logoUrl + '\'' +
                ", reviewCount=" + reviewCount +
                ", totalRating=" + totalRating +
                ", averageRating=" + averageRating +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deleted=" + deleted +
                '}';
    }
}
