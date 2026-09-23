package com.example.backend.service;

import com.example.backend.domain.Game;
import com.example.backend.domain.GameStatus;
import com.example.backend.domain.Guess;
import com.example.backend.exception.GameNotFoundException;
import com.example.backend.exception.InvalidGuessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the local PostgreSQL database and rolls back after every test.
 * The persistence context is cleared where a test needs the game to come back
 * from the database rather than from Hibernate's first-level cache.
 */
@SpringBootTest
@Transactional
class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void submits_a_guess_evaluates_it_and_updates_the_game() {
        Game game = gameService.createGame();

        Game updatedGame = gameService.submitGuess(
                game.getId(),
                game.getAnswer()
        );

        assertThat(updatedGame.getStatus()).isEqualTo(GameStatus.WON);
        assertThat(updatedGame.getAttemptsRemaining()).isEqualTo(5);
        assertThat(updatedGame.getGuesses()).hasSize(1);
    }

    @Test
    void rejects_a_guess_that_is_not_exactly_five_letters() {
        Game game = gameService.createGame();

        assertThatThrownBy(() -> gameService.submitGuess(game.getId(), "CAT"))
                .isInstanceOf(InvalidGuessException.class)
                .hasMessage("Guess must contain exactly 5 letters.");
    }

    @Test
    void rejects_a_five_letter_guess_that_is_not_in_the_dictionary() {
        Game game = gameService.createGame();

        assertThatThrownBy(() -> gameService.submitGuess(game.getId(), "QWERT"))
                .isInstanceOf(InvalidGuessException.class)
                .hasMessage("Guess is not in the dictionary.");
    }

    @Test
    void rejects_a_guess_for_a_game_that_does_not_exist() {
        UUID unknownGameId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        assertThatThrownBy(() -> gameService.submitGuess(unknownGameId, "CRANE"))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessage("Game not found.");
    }

    @Test
    void reports_no_game_for_an_unknown_id() {
        assertThat(gameService.findGame(UUID.randomUUID())).isEmpty();
    }

    @Test
    void reloads_an_unfinished_game_with_its_guesses_from_the_database() {
        Game game = gameService.createGame();
        String guessWord = aWordThatIsNotTheAnswer(game);

        Guess submittedGuess = gameService
                .submitGuess(game.getId(), guessWord)
                .getGuesses()
                .getFirst();

        reloadFromDatabase();

        Game reloadedGame = gameService.getGame(game.getId());

        assertThat(reloadedGame.getId()).isEqualTo(game.getId());
        assertThat(reloadedGame.getAnswer()).isEqualTo(game.getAnswer());
        assertThat(reloadedGame.getMaxAttempts()).isEqualTo(game.getMaxAttempts());
        assertThat(reloadedGame.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(reloadedGame.getAttemptsRemaining()).isEqualTo(5);
        assertThat(reloadedGame.getGuesses()).containsExactly(submittedGuess);
    }

    @Test
    void reloads_a_won_game_with_every_guess_in_order() {
        Game game = gameService.createGame();
        String losingGuessWord = aWordThatIsNotTheAnswer(game);

        gameService.submitGuess(game.getId(), losingGuessWord);
        Game wonGame = gameService.submitGuess(game.getId(), game.getAnswer());

        reloadFromDatabase();

        Game reloadedGame = gameService.getGame(game.getId());

        assertThat(reloadedGame.getStatus()).isEqualTo(GameStatus.WON);
        assertThat(reloadedGame.getAttemptsRemaining()).isEqualTo(4);
        assertThat(reloadedGame.getGuesses())
                .containsExactlyElementsOf(wonGame.getGuesses());
    }

    private void reloadFromDatabase() {
        entityManager.flush();
        entityManager.clear();
    }

    private String aWordThatIsNotTheAnswer(Game game) {
        return game.getAnswer().equals("CRANE") ? "SLATE" : "CRANE";
    }
}
