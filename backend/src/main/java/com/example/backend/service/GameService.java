package com.example.backend.service;

import com.example.backend.domain.Game;
import com.example.backend.domain.Guess;
import com.example.backend.domain.LetterResult;
import com.example.backend.domain.WordleEvaluator;
import com.example.backend.dictionary.WordDictionary;
import com.example.backend.exception.GameNotFoundException;
import com.example.backend.exception.InvalidGuessException;
import com.example.backend.persistence.GameEntity;
import com.example.backend.persistence.GameMapper;
import com.example.backend.persistence.GameRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class GameService {
    private static final int MAX_ATTEMPTS = 6;
    private final WordleEvaluator wordleEvaluator = new WordleEvaluator();
    private final WordDictionary wordDictionary;
    private final GameRepository gameRepository;
    private final GameMapper gameMapper;

    public GameService(
            WordDictionary wordDictionary,
            GameRepository gameRepository,
            GameMapper gameMapper
    ) {
        this.wordDictionary = wordDictionary;
        this.gameRepository = gameRepository;
        this.gameMapper = gameMapper;
    }

    @Transactional
    public Game createGame() {
        String answer = wordDictionary.randomAnswer();

        Game game = new Game(UUID.randomUUID(), answer, MAX_ATTEMPTS, Instant.now());

        gameRepository.save(gameMapper.toEntity(game));

        return game;
    }

    @Transactional(readOnly = true)
    public Optional<Game> findGame(UUID gameId) {
        return gameRepository.findById(gameId).map(gameMapper::toDomain);
    }

    @Transactional(readOnly = true)
    public Game getGame(UUID gameId) {
        return gameMapper.toDomain(loadGameEntity(gameId));
    }

    @Transactional
    public Game submitGuess(UUID gameId, String word) {
        String normalizedWord = validateAndNormalizeGuess(word);

        GameEntity gameEntity = gameRepository.findByIdForPlay(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found."));

        Game game = gameMapper.toDomain(gameEntity);

        List<LetterResult> result = wordleEvaluator.evaluate(
                game.getAnswer(),
                normalizedWord
        );

        Guess guess = new Guess(normalizedWord, result);

        game.addGuess(guess);

        gameMapper.applyTo(gameEntity, game);
        gameRepository.save(gameEntity);

        return game;
    }

    private GameEntity loadGameEntity(UUID gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException("Game not found."));
    }

    private String validateAndNormalizeGuess(String word) {
        if (word == null || !word.matches("[A-Za-z]{5}")) {
            throw new InvalidGuessException(
                    "Guess must contain exactly 5 letters."
            );
        }

        String normalizedWord = word.toUpperCase(Locale.ROOT);

        if (!wordDictionary.isAllowedGuess(normalizedWord)) {
            throw new InvalidGuessException("Guess is not in the dictionary.");
        }

        return normalizedWord;
    }
}
