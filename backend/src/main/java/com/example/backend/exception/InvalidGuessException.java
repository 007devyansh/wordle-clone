package com.example.backend.exception;

public class InvalidGuessException extends RuntimeException {

    public InvalidGuessException(String message) {
        super(message);
    }
}