package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Analysis {

    private String catId;
    private String uid;
    private String catName;
    private String emotion;
    private float probability;
    private List<BodyLanguage> bodyLanguages;
    private String imageUrl;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean rated;
    private boolean deleted;

    public Analysis() {}

    public Analysis(String catId, String uid, String catName, String emotion, float probability, List<BodyLanguage> bodyLanguages, String imageUrl, Timestamp createdAt, Timestamp updatedAt, boolean rated, boolean deleted) {
        this.catId = catId;
        this.uid = uid;
        this.catName = catName;
        this.emotion = emotion;
        this.probability = probability;
        this.bodyLanguages = bodyLanguages;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.rated = rated;
        this.deleted = deleted;
    }

    public String getCatId() {
        return catId;
    }

    public String getUid() {
        return uid;
    }

    public String getCatName() {
        return catName;
    }

    public String getEmotion() {
        return emotion;
    }

    public float getProbability() {
        return probability;
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

    public boolean isRated() {
        return rated;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setCatName(String catName) {
        this.catName = catName;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public void setProbability(float probability) {
        this.probability = probability;
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

    public void setRated(boolean rated) {
        this.rated = rated;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Analysis{" +
                "catId='" + catId + '\'' +
                ", uid='" + uid + '\'' +
                ", catName='" + catName + '\'' +
                ", emotion='" + emotion + '\'' +
                ", probability=" + probability +
                ", bodyLanguages=" + bodyLanguages +
                ", imageUrl='" + imageUrl + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", rated=" + rated +
                ", deleted=" + deleted +
                '}';
    }
}
