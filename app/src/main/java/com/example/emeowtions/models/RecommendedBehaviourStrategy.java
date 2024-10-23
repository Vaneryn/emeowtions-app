package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class RecommendedBehaviourStrategy {

    private String stratId;
    private String description;
    private String recommendationFactorId;
    private String factorType;
    private String factorValue;
    private boolean rated;
    private boolean liked;
    private boolean disliked;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public RecommendedBehaviourStrategy() {}

    public RecommendedBehaviourStrategy(String stratId, String description, String recommendationFactorId, String factorType, String factorValue, boolean rated, boolean liked, boolean disliked, Timestamp createdAt, Timestamp updatedAt) {
        this.stratId = stratId;
        this.description = description;
        this.recommendationFactorId = recommendationFactorId;
        this.factorType = factorType;
        this.factorValue = factorValue;
        this.rated = rated;
        this.liked = liked;
        this.disliked = disliked;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getStratId() {
        return stratId;
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

    public boolean isLiked() {
        return liked;
    }

    public boolean isDisliked() {
        return disliked;
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

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public void setDisliked(boolean disliked) {
        this.disliked = disliked;
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
                ", description='" + description + '\'' +
                ", recommendationFactorId='" + recommendationFactorId + '\'' +
                ", factorType='" + factorType + '\'' +
                ", factorValue='" + factorValue + '\'' +
                ", rated=" + rated +
                ", liked=" + liked +
                ", disliked=" + disliked +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
