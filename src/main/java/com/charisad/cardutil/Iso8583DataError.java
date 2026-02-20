package com.charisad.cardutil;

public class Iso8583DataError extends CardutilError {
    private final Object binaryContextData;

    public Iso8583DataError(String message) {
        super(message);
        this.binaryContextData = null;
    }

    public Iso8583DataError(String message, byte[] binaryContextData, Throwable cause) {
        super(message, cause);
        this.binaryContextData = binaryContextData;
    }
}
