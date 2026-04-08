package com.bofalgan.pharmacy.util;

public class ValidationException extends PharmacyException {
    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public ValidationException(String message) {
        super(message);
        this.field = null;
    }

    public String getField() { return field; }
}
