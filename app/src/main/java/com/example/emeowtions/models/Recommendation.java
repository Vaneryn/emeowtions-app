package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

import java.util.List;

public class Recommendation {

    private String analysisId;
    private List<RecommendedBehaviourStrategy> strategies;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Recommendation() {}

    public Recommendation(String analysisId, List<RecommendedBehaviourStrategy> strategies, Timestamp createdAt, Timestamp updatedAt) {
        this.analysisId = analysisId;
        this.strategies = strategies;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public List<RecommendedBehaviourStrategy> getStrategies() {
        return strategies;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public void setStrategies(List<RecommendedBehaviourStrategy> strategies) {
        this.strategies = strategies;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
