package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Analysis {

    private String catId;
    private String uid;
    private String emotion;
    private List<BodyLanguage> bodyLanguages;
    private String imageUrl;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean forFeedback;

    public Analysis() {}

    public Analysis(String catId, String uid, String emotion, List<BodyLanguage> bodyLanguages, String imageUrl, Timestamp createdAt, Timestamp updatedAt, boolean forFeedback) {
        this.catId = catId;
        this.uid = uid;
        this.emotion = emotion;
        this.bodyLanguages = bodyLanguages;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.forFeedback = forFeedback;
    }

    public String getCatId() {
        return catId;
    }

    public String getUid() {
        return uid;
    }

    public String getEmotion() {
        return emotion;
    }

    public List<BodyLanguage> getBodyLanguages() {
        return bodyLanguages;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public boolean isForFeedback() {
        return forFeedback;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public void setBodyLanguages(List<BodyLanguage> bodyLanguages) {
        this.bodyLanguages = bodyLanguages;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setForFeedback(boolean forFeedback) {
        this.forFeedback = forFeedback;
    }

    @Override
    public String toString() {
        return "Analysis{" +
                "catId='" + catId + '\'' +
                ", uid='" + uid + '\'' +
                ", emotion='" + emotion + '\'' +
                ", bodyLanguages=" + bodyLanguages +
                ", imageUrl='" + imageUrl + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", forFeedback=" + forFeedback +
                '}';
    }
}
