package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.backend.domain.GameStatus;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameResponse(
        UUID gameId,
        List<GuessResponse> guesses,
        int attemptsRemaining,
        GameStatus status,
        String answer
) {
}
