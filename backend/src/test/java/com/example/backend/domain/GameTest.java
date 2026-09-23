package com.example.backend.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameTest {

    @Test
    void marks_the_game_as_won_when_a_guess_is_all_correct() {
        Game game = newGame();

        game.addGuess(new Guess(
                "CRANE",
                List.of(
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT
                )
        ));

        assertThat(game.getStatus()).isEqualTo(GameStatus.WON);
        assertThat(game.getGuesses()).hasSize(1);
    }

    @Test
    void marks_the_game_as_lost_after_six_non_winning_guesses() {
        Game game = newGame();

        for (int attempt = 0; attempt < 6; attempt++) {
            game.addGuess(incorrectGuess());
        }

        assertThat(game.getStatus()).isEqualTo(GameStatus.LOST);
        assertThat(game.getAttemptsRemaining()).isZero();
    }

    @Test
    void rejects_a_guess_after_the_game_has_finished() {
        Game game = newGame();

        game.addGuess(new Guess(
                "CRANE",
                List.of(
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT
                )
        ));

        assertThatThrownBy(() -> game.addGuess(incorrectGuess()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void restores_a_finished_game_from_persisted_state() {
        Guess winningGuess = new Guess(
                "CRANE",
                List.of(
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT,
                        LetterResult.CORRECT
                )
        );

        Game game = Game.restore(
                UUID.randomUUID(),
                "CRANE",
                List.of(winningGuess),
                6,
                GameStatus.WON,
                Instant.now()
        );

        assertThat(game.getGuesses()).containsExactly(winningGuess);
        assertThat(game.getStatus()).isEqualTo(GameStatus.WON);
        assertThat(game.getAttemptsRemaining()).isEqualTo(5);
    }

    @Test
    void rejects_persisted_state_with_a_status_that_does_not_match_its_guesses() {
        assertThatThrownBy(() -> Game.restore(
                UUID.randomUUID(),
                "CRANE",
                List.of(),
                6,
                GameStatus.WON,
                Instant.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Persisted game status does not match its guesses.");
    }

    private Game newGame() {
        return new Game(
                UUID.randomUUID(),
                "CRANE",
                6,
                Instant.now()
        );
    }

    private Guess incorrectGuess() {
        return new Guess(
                "SLATE",
                List.of(
                        LetterResult.ABSENT,
                        LetterResult.ABSENT,
                        LetterResult.CORRECT,
                        LetterResult.ABSENT,
                        LetterResult.CORRECT
                )
        );
    }
}
