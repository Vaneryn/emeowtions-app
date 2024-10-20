package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class VeterinaryStaff {
    private String uid;
    private String veterinaryClinicId;
    private String jobTitle;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean deleted;

    public VeterinaryStaff() {}

    public VeterinaryStaff(String uid, String veterinaryClinicId, String jobTitle, Timestamp createdAt, Timestamp updatedAt, boolean deleted) {
        this.uid = uid;
        this.veterinaryClinicId = veterinaryClinicId;
        this.jobTitle = jobTitle;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public String getUid() {
        return uid;
    }

    public String getVeterinaryClinicId() {
        return veterinaryClinicId;
    }

    public String getJobTitle() {
        return jobTitle;
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

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setVeterinaryClinicId(String veterinaryClinicId) {
        this.veterinaryClinicId = veterinaryClinicId;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
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
