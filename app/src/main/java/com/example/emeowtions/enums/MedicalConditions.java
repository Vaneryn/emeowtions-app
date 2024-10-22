package com.example.emeowtions.enums;

public enum MedicalConditions {
    YES("Yes", 1),
    NO("No", 2),
    UNSPECIFIED("Unspecified", 3);

    private final String title;
    private final int code;

    MedicalConditions(String title, int code) {
        this.title = title;
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public int getCode() {
        return code;
    }
}
