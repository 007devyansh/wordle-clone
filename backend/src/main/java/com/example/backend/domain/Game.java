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

    public static Game restore(
            UUID id,
            String answer,
            List<Guess> guesses,
            int maxAttempts,
            GameStatus status,
            Instant createdAt
    ) {
        Game game = new Game(id, answer, maxAttempts, createdAt);

        guesses.forEach(game::addGuess);

        if (game.status != status) {
            throw new IllegalArgumentException(
                    "Persisted game status does not match its guesses."
            );
        }

        return game;
    }

    public void addGuess(Guess guess) {
        if (status != GameStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot add a guess to a finished game.");
        }

        if (guesses.size() >= maxAttempts) {
            throw new IllegalStateException("No attempts remain.");
        }

        guesses.add(guess);

        if (isWinningGuess(guess)) {
            status = GameStatus.WON;
        } else if (guesses.size() >= maxAttempts) {
            status = GameStatus.LOST;
        }
    }

    private boolean isWinningGuess(Guess guess) {
        return guess.result().stream()
                .allMatch(result -> result == LetterResult.CORRECT);
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

    public int getAttemptsRemaining() {
        return maxAttempts - guesses.size();
    }

    public GameStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
