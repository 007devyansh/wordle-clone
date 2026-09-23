package com.example.backend.exception;

import com.example.backend.dto.ApiErrorResponse;
import com.example.backend.persistence.GuessEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void maps_a_stale_game_version_to_a_concurrent_guess_conflict() {
        ResponseEntity<ApiErrorResponse> response = handler.handleConcurrentGuess(
                new ObjectOptimisticLockingFailureException("GameEntity", "some-id")
        );

        assertConcurrentGuessConflict(response);
    }

    @Test
    void maps_a_duplicate_attempt_number_to_a_concurrent_guess_conflict() {
        ResponseEntity<ApiErrorResponse> response = handler.handleDataIntegrityViolation(
                violationOf(GuessEntity.ONE_GUESS_PER_ATTEMPT_CONSTRAINT)
        );

        assertConcurrentGuessConflict(response);
    }

    @Test
    void leaves_any_other_constraint_violation_as_a_server_error() {
        DataIntegrityViolationException unrelated = violationOf("guesses_word_check");

        assertThatThrownBy(() -> handler.handleDataIntegrityViolation(unrelated))
                .isSameAs(unrelated);
    }

    private static DataIntegrityViolationException violationOf(String constraintName) {
        return new DataIntegrityViolationException(
                "could not execute statement",
                new ConstraintViolationException(
                        "could not execute statement",
                        new SQLException("duplicate key value"),
                        constraintName
                )
        );
    }

    private static void assertConcurrentGuessConflict(ResponseEntity<ApiErrorResponse> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONCURRENT_GUESS");
    }
}
