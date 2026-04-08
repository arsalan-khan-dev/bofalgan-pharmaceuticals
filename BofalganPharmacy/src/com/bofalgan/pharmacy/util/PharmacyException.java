package com.bofalgan.pharmacy.util;

/** Base exception for all pharmacy system errors. */
public class PharmacyException extends RuntimeException {
    public PharmacyException(String message) { super(message); }
    public PharmacyException(String message, Throwable cause) { super(message, cause); }
}
