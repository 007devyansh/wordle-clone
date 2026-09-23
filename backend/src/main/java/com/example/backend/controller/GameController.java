package com.example.backend.controller;

import com.example.backend.domain.Game;
import com.example.backend.domain.Guess;
import com.example.backend.dto.*;
import com.example.backend.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private  final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<CreateGameResponse> createGame() {
        Game game = gameService.createGame();

        CreateGameResponse response = new CreateGameResponse (
                game.getId(),
                game.getMaxAttempts(),
                game.getStatus()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGame(@PathVariable UUID gameId) {
        Game game = gameService.getGame(gameId);

        return ResponseEntity.ok(toGameResponse(game));
    }

    private GameResponse toGameResponse(Game game) {
        return new GameResponse(
                game.getId(),
                game.getGuesses().stream()
                        .map(guess -> new GuessResponse(
                                guess.word(),
                                guess.result()
                        ))
                        .toList(),
                game.getAttemptsRemaining(),
                game.getStatus(),
                revealedAnswer(game)
        );
    }

    @PostMapping("/{gameId}/guesses")
    public ResponseEntity<SubmitGuessResponse> submitGuess(
            @PathVariable UUID gameId,
            @RequestBody SubmitGuessRequest request
    ) {
        Game updatedGame = gameService.submitGuess(gameId, request.word());

        Guess latestGuess = updatedGame.getGuesses().getLast();

        SubmitGuessResponse response = new SubmitGuessResponse(
                latestGuess.word(),
                latestGuess.result(),
                updatedGame.getAttemptsRemaining(),
                updatedGame.getStatus(),
                revealedAnswer(updatedGame)
        );

        return ResponseEntity.ok(response);
    }

    private String revealedAnswer(Game game) {
        return game.getStatus() == com.example.backend.domain.GameStatus.LOST
                ? game.getAnswer()
                : null;
    }
}
