package com.example.backend.persistence;

import com.example.backend.domain.Game;
import com.example.backend.domain.GameStatus;
import com.example.backend.domain.Guess;
import com.example.backend.dto.ApiErrorResponse;
import com.example.backend.exception.ApiExceptionHandler;
import com.example.backend.service.GameService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Deliberately not transactional: every service call commits on its own, so the
 * later reads run in a new transaction with a new persistence context and can
 * only be served by rows that are actually stored in PostgreSQL. That is the
 * same path the application takes after a restart.
 */
@SpringBootTest
@ActiveProfiles("test")
class GamePersistenceIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApiExceptionHandler apiExceptionHandler;

    private final List<UUID> createdGameIds = new ArrayList<>();

    @AfterEach
    void removeCreatedGames() {
        createdGameIds.forEach(gameRepository::deleteById);
    }

    @Test
    void an_unfinished_game_is_readable_by_a_later_independent_transaction() {
        Game game = createGame();
        String guessWord = aWordThatIsNotTheAnswer(game);

        Guess submittedGuess = gameService
                .submitGuess(game.getId(), guessWord)
                .getGuesses()
                .getFirst();

        Game reloadedGame = gameService.getGame(game.getId());

        assertThat(reloadedGame.getAnswer()).isEqualTo(game.getAnswer());
        assertThat(reloadedGame.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
        assertThat(reloadedGame.getAttemptsRemaining()).isEqualTo(5);
        assertThat(reloadedGame.getGuesses()).containsExactly(submittedGuess);
    }

    @Test
    void a_finished_game_keeps_its_status_and_full_history() {
        Game game = createGame();

        gameService.submitGuess(game.getId(), aWordThatIsNotTheAnswer(game));
        Game wonGame = gameService.submitGuess(game.getId(), game.getAnswer());

        Game reloadedGame = gameService.getGame(game.getId());

        assertThat(reloadedGame.getStatus()).isEqualTo(GameStatus.WON);
        assertThat(reloadedGame.getGuesses())
                .containsExactlyElementsOf(wonGame.getGuesses());
    }

    @Test
    void stores_the_guess_rows_the_schema_expects() {
        Game game = createGame();

        Guess submittedGuess = gameService
                .submitGuess(game.getId(), aWordThatIsNotTheAnswer(game))
                .getGuesses()
                .getFirst();

        Map<String, Object> guessRow = jdbcTemplate.queryForMap(
                "SELECT id, attempt_number, word FROM guesses WHERE game_id = ?",
                game.getId()
        );

        assertThat(guessRow.get("attempt_number")).isEqualTo(1);
        assertThat(guessRow.get("word")).isEqualTo(submittedGuess.word());

        List<String> storedResults = jdbcTemplate.queryForList(
                "SELECT letter_result FROM guess_results WHERE guess_id = ? ORDER BY letter_index",
                String.class,
                guessRow.get("id")
        );

        assertThat(storedResults).isEqualTo(
                submittedGuess.result().stream().map(Enum::name).toList()
        );
    }

    @Test
    void bumps_the_optimistic_locking_version_on_every_guess() {
        Game game = createGame();

        assertThat(storedVersionOf(game)).isZero();

        gameService.submitGuess(game.getId(), aWordThatIsNotTheAnswer(game));

        assertThat(storedVersionOf(game)).isEqualTo(1L);
    }

    /**
     * Replays a double submit deterministically on one thread. The slower request
     * reads the game and applies its guess, but has not flushed yet when a competing
     * request reads the same state and commits first. Both chose attempt 1, so the
     * slower commit must fail and be reported as a conflict rather than a 500.
     */
    @Test
    void rejects_the_slower_of_two_racing_guesses_as_a_conflict() {
        Game game = createGame();
        String guessWord = aWordThatIsNotTheAnswer(game);

        TransactionTemplate slowerRequest = new TransactionTemplate(transactionManager);
        TransactionTemplate fasterRequest = new TransactionTemplate(transactionManager);
        fasterRequest.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Throwable failure = catchThrowable(() -> slowerRequest.executeWithoutResult(status -> {
            gameService.submitGuess(game.getId(), guessWord);
            fasterRequest.executeWithoutResult(
                    inner -> gameService.submitGuess(game.getId(), guessWord)
            );
        }));

        assertThat(failure).isInstanceOfAny(
                OptimisticLockingFailureException.class,
                DataIntegrityViolationException.class
        );

        ResponseEntity<ApiErrorResponse> response = failure instanceof OptimisticLockingFailureException lockFailure
                ? apiExceptionHandler.handleConcurrentGuess(lockFailure)
                : apiExceptionHandler.handleDataIntegrityViolation((DataIntegrityViolationException) failure);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONCURRENT_GUESS");

        Game storedGame = gameService.getGame(game.getId());

        assertThat(storedGame.getGuesses()).hasSize(1);
        assertThat(storedVersionOf(game)).isEqualTo(1L);
    }

    private Long storedVersionOf(Game game) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM games WHERE id = ?",
                Long.class,
                game.getId()
        );
    }

    private Game createGame() {
        Game game = gameService.createGame();

        createdGameIds.add(game.getId());

        return game;
    }

    private String aWordThatIsNotTheAnswer(Game game) {
        return game.getAnswer().equals("CRANE") ? "SLATE" : "CRANE";
    }
}
