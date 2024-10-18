package com.example.emeowtions.models;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

public class VeterinaryClinicRegistration {
    private String name;
    private String description;
    private String address;
    private String email;
    private String phoneNumber;
    private String logoUrl;
    private String status;
    private String rejectionReason;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public VeterinaryClinicRegistration() {}

    public VeterinaryClinicRegistration(String name, String description, String address, String email, String phoneNumber, String logoUrl, String status, String rejectionReason, Timestamp createdAt, Timestamp updatedAt) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.logoUrl = logoUrl;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
