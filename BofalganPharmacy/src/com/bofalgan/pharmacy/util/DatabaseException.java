package com.bofalgan.pharmacy.util;

public class DatabaseException extends PharmacyException {
    public DatabaseException(String message) { super(message); }
    public DatabaseException(String message, Throwable cause) { super(message, cause); }
}
