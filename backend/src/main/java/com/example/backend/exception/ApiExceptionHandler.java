package com.example.backend.exception;

import com.example.backend.dto.ApiErrorResponse;
import com.example.backend.persistence.GuessEntity;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String CONCURRENT_GUESS_MESSAGE =
            "Another guess was submitted for this game at the same time. Reload the game and try again.";

    @ExceptionHandler(InvalidGuessException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidGuess(
            InvalidGuessException exception
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_GUESS",
                exception.getMessage()
        );
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleGameNotFound(
            GameNotFoundException exception
    ) {
        return error(
                HttpStatus.NOT_FOUND,
                "GAME_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleGameConflict(
            IllegalStateException exception
    ) {
        return error(
                HttpStatus.CONFLICT,
                "GAME_FINISHED",
                exception.getMessage()
        );
    }

    /**
     * Two guesses racing on the same game both read the same attempt count. The
     * loser fails at commit, either on the forced games.version check or, when its
     * guess row is flushed first, on the one-guess-per-attempt unique constraint.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrentGuess(
            OptimisticLockingFailureException exception
    ) {
        return concurrentGuess();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        if (!violatesOneGuessPerAttempt(exception)) {
            // Any other violation is a genuine server-side bug, not a client conflict.
            throw exception;
        }

        return concurrentGuess();
    }

    private ResponseEntity<ApiErrorResponse> concurrentGuess() {
        return error(
                HttpStatus.CONFLICT,
                "CONCURRENT_GUESS",
                CONCURRENT_GUESS_MESSAGE
        );
    }

    private static boolean violatesOneGuessPerAttempt(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return GuessEntity.ONE_GUESS_PER_ATTEMPT_CONSTRAINT
                        .equalsIgnoreCase(violation.getConstraintName());
            }
        }

        return false;
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String error,
            String message
    ) {
        return ResponseEntity
                .status(status)
                .body(new ApiErrorResponse(error, message));
    }
}
