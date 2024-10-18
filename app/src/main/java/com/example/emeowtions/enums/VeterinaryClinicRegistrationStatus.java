package com.example.emeowtions.enums;

public enum VeterinaryClinicRegistrationStatus {
    PENDING("Pending", 1),
    APPROVED("Approved", 2),
    REJECTED("Rejected", 3);

    private final String title;
    private final int code;

    VeterinaryClinicRegistrationStatus(String title, int code) {
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
