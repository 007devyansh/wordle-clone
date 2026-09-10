package com.example.backend.service;

import com.example.backend.domain.Game;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameService {
    private static final int MAX_ATTEMPTS = 6;
    private final List<String> wordPool = List.of(
            "CRANE",
            "SLATE",
            "STARE",
            "PLANT",
            "HOUSE",
            "BRICK"
    );

    private final Map<UUID, Game> games = new ConcurrentHashMap<>();

    public Game createGame() {
        String answer = chooseAnswer();

        Game game = new Game(UUID.randomUUID(), answer, MAX_ATTEMPTS, Instant.now());

        games.put(game.getId(), game);
        return game;
    }

    public Optional<Game> findGame(UUID gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    private String chooseAnswer() {
        int randomIndex = ThreadLocalRandom.current().nextInt(wordPool.size());
        return wordPool.get(randomIndex);
    }
}
