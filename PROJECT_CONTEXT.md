# Wordle Clone — Project Context

## Product and architecture decisions

- This is an **unlimited**, anonymous, single-player Wordle game. Every new game receives a random backend-selected answer; there is no daily puzzle mode.
- The Next.js frontend owns UI state only (typed input, board rendering, animations, keyboard state, and the current `gameId` in local storage).
- The Spring Boot backend is authoritative for game state (answer, guesses, attempts, evaluation, and win/loss status).
- The browser must never receive the answer. API DTOs—not domain objects or entities—control the public response shape.
- Current V1 features intentionally include a backend word dictionary and dictionary-valid guess checking.
- Out of scope for now: accounts, game ownership/history, stats, daily puzzles, deployment, Docker, and multiplayer.

## Current implementation state

### V1 game flow

- `POST /api/games` creates a game.
- `GET /api/games/{gameId}` restores a game.
- `POST /api/games/{gameId}/guesses` validates and evaluates a guess.
- The backend supports six attempts, win/loss states, structured error responses, and duplicate-letter evaluation. Game state is stored in PostgreSQL (see below).
- The frontend is in `frontend/` and restores the active game using local storage.

### PostgreSQL phase (complete)

Game state now lives in PostgreSQL; the `ConcurrentHashMap` is gone.

Local database setup:

- PostgreSQL 16 was installed with Homebrew and started as `postgresql@16`.
- PostgreSQL role: `wordle_app` (application login; password is intentionally not recorded here).
- Database: `wordle`, owned by `wordle_app`.
- `backend/src/main/resources/application.properties` connects to `jdbc:postgresql://localhost:5432/wordle` and reads the password from `WORDLE_DB_PASSWORD`.
- Maven dependencies include Spring Data JPA, the PostgreSQL JDBC driver, Flyway Core, and Flyway PostgreSQL support.

Migrations (`flyway_schema_history` records versions 1 and 2 as successful):

- `V1__create_wordle_schema.sql` created `games`, `guesses`, and `guess_results`.
- `V2__align_numeric_column_types.sql` widened `max_attempts`, `attempt_number`, and `letter_index` from `SMALLINT` to `INTEGER`, because Hibernate's `ddl-auto=validate` maps Java `int` to `INTEGER`.

The schema:

```text
games
  id, answer, max_attempts, status, created_at, version

guesses
  id, game_id, attempt_number, word

guess_results
  guess_id, letter_index, letter_result
```

Persistence code (all in `backend/src/main/java/com/example/backend/persistence/`):

- `GameEntity` maps `games`, owns its guesses through a `@OneToMany` ordered by `attempt_number` with `cascade = ALL` and `orphanRemoval`, and carries `@Version` on `games.version`.
- `GuessEntity` maps `guesses` and stores its ordered `LetterResult` list as an `@ElementCollection` in `guess_results`, ordered by `@OrderColumn(name = "letter_index")`.
- `GameRepository` is a `JpaRepository`. It adds `findByIdForPlay(...)`, which loads a game with `OPTIMISTIC_FORCE_INCREMENT`: appending a guess only writes child rows, so without the forced bump `games.version` would never change and two racing guesses could both be accepted.
- `GameMapper` converts entities to domain objects through `Game.restore(...)` and copies a played turn back onto a loaded entity by setting the status and appending only the guesses that are new.

`Game.restore(...)` rehydrates a game from persisted guesses, derives the status by replaying the existing rules, and rejects stored data whose status contradicts its guesses.

`GameService` is now transactional: `createGame` saves a mapped entity, `getGame`/`findGame` are read-only lookups, and `submitGuess` loads the entity, maps it to the domain, applies the Wordle rule, maps the change back, and saves. Controller routes and DTOs are unchanged.

### Concurrent guesses

Two guesses racing on the same game both read the same attempt count; the one that commits second fails. In practice it fails on the `guesses_game_id_attempt_number_key` unique constraint (Hibernate flushes the guess insert before the forced version update), otherwise on the version check. `ApiExceptionHandler` maps both to `409 CONCURRENT_GUESS`. Any other `DataIntegrityViolationException` is rethrown and stays a 500, since it would be a real bug. `GamePersistenceIntegrationTest.rejects_the_slower_of_two_racing_guesses_as_a_conflict` replays the race deterministically on one thread; `ApiExceptionHandlerTest` covers the mapping without a database.

### Verification

- `WORDLE_DB_PASSWORD=... mvn test` runs 27 tests, all passing. This includes `GameServiceTest` (`@SpringBootTest @Transactional`, rolling back and clearing the persistence context to force real database reads) and `GamePersistenceIntegrationTest` (non-transactional, so every service call commits and later reads come from committed rows; it deletes the games it creates).
- A real restart was verified end to end: the packaged jar was started, a game was created and guessed against over HTTP, the JVM was stopped, and a fresh JVM served `GET /api/games/{id}` with the earlier guess intact and accepted a further guess.

## Important current limitations

- The integration tests run against the local development `wordle` database rather than a dedicated test database. They clean up the rows they create, but there is no isolation from development data.
- The frontend's double-submit guard in `app/page.tsx` uses React state (`isSubmitting`), so two Enter presses within one render can both send a request. The backend rejects the second with a 409, but a ref-based guard would stop the UI from causing it.
- Finished games are never removed; the `games` table grows without bound.
- `pom.xml` targets Java 21, but the local runtime currently reports Java 26. This is non-blocking so far, but should be aligned to Java 21 later.

## Exact next step

Pick up one of the limitations above. The most useful next pieces are the ref-based double-submit guard in the frontend, and giving the integration tests their own database instead of the development one.

## Running locally

Start PostgreSQL if needed:

```bash
brew services start postgresql@16
```

From `backend/`, set the password for the current terminal and run the app:

```bash
export WORDLE_DB_PASSWORD='your-local-database-password'
mvn spring-boot:run
```

Stop PostgreSQL later with:

```bash
brew services stop postgresql@16
```
