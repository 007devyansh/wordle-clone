package com.example.backend.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<GameEntity, UUID> {

    /**
     * Loads a game that is about to be played. Appending a guess only writes child
     * rows, which on its own leaves games.version untouched, so the version is
     * incremented deliberately: two guesses racing on the same game then make one
     * of them fail instead of both being accepted.
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select game from GameEntity game where game.id = :gameId")
    Optional<GameEntity> findByIdForPlay(@Param("gameId") UUID gameId);
}
