package com.charisad.cardutil;

public class CardutilError extends RuntimeException {
    public CardutilError(String message) {
        super(message);
    }

    public CardutilError(String message, Throwable cause) {
        super(message, cause);
    }   
}
