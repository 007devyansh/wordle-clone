package com.example.backend.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Game {
    private final UUID id;
    private final String answer;
    private final List<Guess> guesses;
    private final int maxAttempts;
    private GameStatus status;
    private final Instant createdAt;


    public Game(UUID id, String answer, int maxAttempts, Instant createdAt) {
        this.id = id;
        this.answer = answer;
        this.guesses = new ArrayList<>();
        this.maxAttempts = maxAttempts;
        this.createdAt = createdAt;
        this.status = GameStatus.IN_PROGRESS;
    }

    public UUID getId() {
        return id;
    }

    public String getAnswer() {
        return answer;
    }

    public List<Guess> getGuesses() {
        return List.copyOf(guesses);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
