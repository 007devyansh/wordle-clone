package com.example.backend.domain;

import java.util.List;

public record Guess (
        String word,
        List<LetterResult> result
) {
    public Guess {
        result = List.copyOf(result);
    }
}
