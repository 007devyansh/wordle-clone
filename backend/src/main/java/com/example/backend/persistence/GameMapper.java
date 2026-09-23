package com.example.backend.persistence;

import com.example.backend.domain.Game;
import com.example.backend.domain.Guess;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Translates between the persistence entities and the Wordle domain model so that
 * the domain classes stay free of Hibernate annotations.
 */
@Component
public class GameMapper {

    public Game toDomain(GameEntity entity) {
        List<Guess> guesses = entity.getGuesses().stream()
                .map(guess -> new Guess(guess.getWord(), guess.getResults()))
                .toList();

        return Game.restore(
                entity.getId(),
                entity.getAnswer(),
                guesses,
                entity.getMaxAttempts(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public GameEntity toEntity(Game game) {
        GameEntity entity = new GameEntity(
                game.getId(),
                game.getAnswer(),
                game.getMaxAttempts(),
                game.getStatus(),
                game.getCreatedAt()
        );

        appendNewGuesses(entity, game);

        return entity;
    }

    /**
     * Copies the state a played turn can change back onto a loaded entity. The entity
     * already holds every guess the domain game was restored from, so only the guesses
     * added since then are appended.
     */
    public void applyTo(GameEntity entity, Game game) {
        entity.setStatus(game.getStatus());

        appendNewGuesses(entity, game);
    }

    private void appendNewGuesses(GameEntity entity, Game game) {
        List<Guess> guesses = game.getGuesses();

        for (int index = entity.getGuesses().size(); index < guesses.size(); index++) {
            Guess guess = guesses.get(index);

            entity.addGuess(new GuessEntity(
                    UUID.randomUUID(),
                    index + 1,
                    guess.word(),
                    guess.result()
            ));
        }
    }
}
