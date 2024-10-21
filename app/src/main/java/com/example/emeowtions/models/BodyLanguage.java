package com.example.emeowtions.models;

import com.google.firebase.Timestamp;

public class BodyLanguage {

    private String key;
    private String value;
    private String description;
    private int probability;

    public BodyLanguage() {

    }

    public BodyLanguage(String key, String value, String description, int probability) {
        this.key = key;
        this.value = value;
        this.description = description;
        this.probability = probability;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public int getProbability() {
        return probability;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProbability(int probability) {
        this.probability = probability;
    }
}
