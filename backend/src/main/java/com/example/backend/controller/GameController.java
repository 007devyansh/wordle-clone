package com.example.backend.controller;

import com.example.backend.domain.Game;
import com.example.backend.dto.CreateGameResponse;
import com.example.backend.service.GameService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
