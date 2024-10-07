package com.example.emeowtions.enums;

public enum CatBackground {
    DOMESTIC("Domestic", 1),
    STRAY("Stray", 2),
    FERAL("Feral", 3);

    private final String title;
    private final int code;

    CatBackground(String title, int code) {
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
