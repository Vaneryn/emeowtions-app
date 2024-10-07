package com.example.emeowtions.enums;

public enum Role {
    ADMIN("Administrator", 1),
    VETERINARY_STAFF("Veterinary Staff", 2),
    VETERINARIAN("Veterinarian", 3),
    USER("User", 4);

    private final String title;
    private final int code;

    Role(String title, int code) {
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
