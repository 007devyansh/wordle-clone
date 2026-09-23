package com.example.backend.dto;

import com.example.backend.domain.LetterResult;

import java.util.List;

public record GuessResponse(
        String word,
        List<LetterResult> result
) {
}