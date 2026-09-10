package com.example.backend.dto;

import com.example.backend.domain.GameStatus;

import java.util.UUID;

public record CreateGameResponse (
        UUID gameId,
        int maxAttempts,
        GameStatus status
){}
