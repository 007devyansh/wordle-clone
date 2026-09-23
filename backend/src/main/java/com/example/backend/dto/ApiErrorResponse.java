package com.example.backend.dto;

public record ApiErrorResponse(
        String error,
        String message
) {
}