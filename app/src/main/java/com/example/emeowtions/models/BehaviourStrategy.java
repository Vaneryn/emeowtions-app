package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class BehaviourStrategy {

    private String id;
    private String description;
    private String recommendationFactorId;
    private String factorType;
    private String factorValue;
    private String emotionType;
    private long likeCount;
    private long dislikeCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public BehaviourStrategy() {}

    public BehaviourStrategy(String id, String description, String recommendationFactorId, String factorType, String factorValue, String emotionType, long likeCount, long dislikeCount, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.description = description;
        this.recommendationFactorId = recommendationFactorId;
        this.factorType = factorType;
        this.factorValue = factorValue;
        this.emotionType = emotionType;
        this.likeCount = likeCount;
        this.dislikeCount = dislikeCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getRecommendationFactorId() {
        return recommendationFactorId;
    }

    public String getFactorType() {
        return factorType;
    }

    public String getFactorValue() {
        return factorValue;
    }

    public String getEmotionType() {
        return emotionType;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRecommendationFactorId(String recommendationFactorId) {
        this.recommendationFactorId = recommendationFactorId;
    }

    public void setFactorType(String factorType) {
        this.factorType = factorType;
    }

    public void setFactorValue(String factorValue) {
        this.factorValue = factorValue;
    }

    public void setEmotionType(String emotionType) {
        this.emotionType = emotionType;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public void setDislikeCount(long dislikeCount) {
        this.dislikeCount = dislikeCount;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "BehaviourStrategy{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", recommendationFactorId='" + recommendationFactorId + '\'' +
                ", factorType='" + factorType + '\'' +
                ", factorValue='" + factorValue + '\'' +
                ", emotionType='" + emotionType + '\'' +
                ", likeCount=" + likeCount +
                ", dislikeCount=" + dislikeCount +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
