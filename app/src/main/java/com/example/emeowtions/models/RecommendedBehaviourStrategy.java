package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class RecommendedBehaviourStrategy {

    private String stratId;
    private String title;
    private String description;
    private String recommendationFactorId;
    private String factorType;
    private String factorValue;
    private boolean rated;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public RecommendedBehaviourStrategy() {}

    public RecommendedBehaviourStrategy(String stratId, String title, String description, String recommendationFactorId, String factorType, String factorValue, boolean rated, Timestamp createdAt, Timestamp updatedAt) {
        this.stratId = stratId;
        this.title = title;
        this.description = description;
        this.recommendationFactorId = recommendationFactorId;
        this.factorType = factorType;
        this.factorValue = factorValue;
        this.rated = rated;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getStratId() {
        return stratId;
    }

    public String getTitle() {
        return title;
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

    public boolean isRated() {
        return rated;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setStratId(String stratId) {
        this.stratId = stratId;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public void setRated(boolean rated) {
        this.rated = rated;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "RecommendedBehaviourStrategy{" +
                "stratId='" + stratId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", recommendationFactorId='" + recommendationFactorId + '\'' +
                ", factorType='" + factorType + '\'' +
                ", factorValue='" + factorValue + '\'' +
                ", rated=" + rated +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
