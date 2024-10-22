package com.example.emeowtions.models;

public class Breed {

    private String name;
    private String temperament;
    private String size;
    private String activityLevel;
    private String grooming;
    private String description;

    public Breed() {}

    public Breed(String name, String temperament, String size, String activityLevel, String grooming, String description) {
        this.name = name;
        this.temperament = temperament;
        this.size = size;
        this.activityLevel = activityLevel;
        this.grooming = grooming;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getTemperament() {
        return temperament;
    }

    public String getSize() {
        return size;
    }

    public String getActivityLevel() {
        return activityLevel;
    }

    public String getGrooming() {
        return grooming;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTemperament(String temperament) {
        this.temperament = temperament;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public void setGrooming(String grooming) {
        this.grooming = grooming;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Breed{" +
                "name='" + name + '\'' +
                ", temperament='" + temperament + '\'' +
                ", size='" + size + '\'' +
                ", activityLevel='" + activityLevel + '\'' +
                ", grooming='" + grooming + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
