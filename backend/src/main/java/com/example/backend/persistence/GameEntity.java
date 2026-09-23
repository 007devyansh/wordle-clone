package com.example.backend.persistence;

import com.example.backend.domain.GameStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 5)
    private String answer;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attemptNumber ASC")
    private List<GuessEntity> guesses = new ArrayList<>();

    protected GameEntity() {
    }

    public GameEntity(
            UUID id,
            String answer,
            int maxAttempts,
            GameStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.answer = answer;
        this.maxAttempts = maxAttempts;
        this.status = status;
        this.createdAt = createdAt;
    }

    void setStatus(GameStatus status) {
        this.status = status;
    }

    public void addGuess(GuessEntity guess) {
        guesses.add(guess);
        guess.setGame(this);
    }

    public UUID getId() {
        return id;
    }

    public String getAnswer() {
        return answer;
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

    public List<GuessEntity> getGuesses() {
        return List.copyOf(guesses);
    }
}
