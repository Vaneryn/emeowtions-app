package com.example.emeowtions.enums;

public enum Role {
    SUPER_ADMIN("Super Admin", 1),
    ADMIN("Admin", 2),
    VETERINARY_STAFF("Veterinary Staff", 3),
    VETERINARIAN("Veterinarian", 4),
    USER("User", 5);

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
