package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.backend.domain.GameStatus;
import com.example.backend.domain.LetterResult;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmitGuessResponse (
        String word,
        List<LetterResult> result,
        int attemptsRemaining,
        GameStatus status,
        String answer
) {
}
