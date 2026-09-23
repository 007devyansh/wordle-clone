package com.example.backend.persistence;

import com.example.backend.domain.LetterResult;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "guesses")
public class GuessEntity {

    /** The V1 unique constraint that allows one guess per attempt number in a game. */
    public static final String ONE_GUESS_PER_ATTEMPT_CONSTRAINT =
            "guesses_game_id_attempt_number_key";

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(nullable = false, length = 5)
    private String word;

    @ElementCollection
    @CollectionTable(name = "guess_results", joinColumns = @JoinColumn(name = "guess_id"))
    @OrderColumn(name = "letter_index")
    @Enumerated(EnumType.STRING)
    @Column(name = "letter_result", nullable = false, length = 10)
    private List<LetterResult> results = new ArrayList<>();

    protected GuessEntity() {
    }

    public GuessEntity(
            UUID id,
            int attemptNumber,
            String word,
            List<LetterResult> results
    ) {
        this.id = id;
        this.attemptNumber = attemptNumber;
        this.word = word;
        this.results.addAll(results);
    }

    void setGame(GameEntity game) {
        this.game = game;
    }

    public UUID getId() {
        return id;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getWord() {
        return word;
    }

    public List<LetterResult> getResults() {
        return List.copyOf(results);
    }
}
