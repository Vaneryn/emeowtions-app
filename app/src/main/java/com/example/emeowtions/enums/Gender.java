package com.example.emeowtions.enums;

public enum Gender {
    MALE("Male", 1),
    FEMALE("Female", 2),
    UNSPECIFIED("Unspecified", 3);

    private final String title;
    private final int code;

    Gender(String title, int code) {
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
