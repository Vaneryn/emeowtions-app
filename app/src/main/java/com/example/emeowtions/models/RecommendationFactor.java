package com.example.emeowtions.models;

public class RecommendationFactor {

    private String type;
    private String value;

    public RecommendationFactor() {}

    public RecommendationFactor(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "RecommendationFactor{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
