# Wordle Clone — Project Context

## Product and architecture decisions

- This is an **unlimited**, anonymous, single-player Wordle game. Every new game receives a random backend-selected answer; there is no daily puzzle mode.
- The Next.js frontend owns UI state only (typed input, board rendering, animations, keyboard state, and the current `gameId` in local storage).
- The Spring Boot backend is authoritative for game state (answer, guesses, attempts, evaluation, and win/loss status).
- The browser must never receive the answer. API DTOs—not domain objects or entities—control the public response shape.
- Current V1 features intentionally include a backend word dictionary and dictionary-valid guess checking.
- Deployment is now in scope (decided 2026-09-23), on free tiers: frontend on Vercel (Hobby), backend on Render (free web service, built from `backend/Dockerfile` because Render has no native Java runtime; no local Docker needed), database on Neon (free plan, permanent). Render's own free PostgreSQL was rejected because it is deleted after 30 days; Railway and DigitalOcean were rejected because they are not free for a Java backend.
- Out of scope for now: accounts, game ownership/history, stats, daily puzzles, Docker, and multiplayer.

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
- Database: `wordle`, owned by `wordle_app`, used by the running app.
- Database: `wordle_test`, owned by `wordle_app`, used only by tests (see Verification).
- `backend/src/main/resources/application.properties` connects to `jdbc:postgresql://localhost:5432/wordle` by default (see Configuration).
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

### Dictionary

Two word lists in `backend/src/main/resources/dictionary/`, like the original Wordle:

- `answers.txt`: 2,023 common words that can be chosen as the answer. Built from SCOWL size levels 10–35, with plurals, past tenses, slurs and obscure forms removed.
- `allowed-guesses.txt`: 8,843 words a player may guess (ENABLE, public domain, plus SCOWL up to level 70). Every answer is also an allowed guess; `WordDictionary` refuses to start otherwise.
- `NOTICE.txt` records the sources and the SCOWL copyright notice, which its license requires to be kept with the lists.

`WordDictionary` exposes `randomAnswer()` and `isAllowedGuess(...)`.

### Configuration

Values that differ between environments are read from environment variables, with local defaults written as `${VARIABLE:default}` in `application.properties`:

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | Port the backend listens on; hosts set this. |
| `WORDLE_DB_URL` | `jdbc:postgresql://localhost:5432/wordle` | JDBC URL. Must start with `jdbc:postgresql://`; hosts often give a `postgres://` URL that needs converting. |
| `WORDLE_DB_USERNAME` | `wordle_app` | Database login. |
| `WORDLE_DB_PASSWORD` | none, required | Database password. |
| `WORDLE_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated browser origins allowed to call `/api/**`. |

Frontend: `NEXT_PUBLIC_API_URL` (default `http://localhost:8080`) is baked into the JavaScript at build time, so it must be set in the hosting settings before the frontend is built.

Spring Boot Actuator exposes only `GET /actuator/health`, which returns `{"status":"UP"}` when the app and its database connection work. Hosts use it to decide whether a deploy is healthy.

### Verification

- Tests use their own database. `backend/src/test/resources/application-test.properties` overrides only `spring.datasource.url` to point at `wordle_test`; every `@SpringBootTest` class carries `@ActiveProfiles("test")` to load it. Flyway builds the schema there from the same migrations. A new Spring test class without `@ActiveProfiles("test")` would silently fall back to the development `wordle` database.
- `WORDLE_DB_PASSWORD=... mvn test` runs 31 tests, all passing. `DeploymentConfigTest` checks the health endpoint and that CORS follows the configured origin.
- The packaged jar was run with `PORT=9090` and `WORDLE_CORS_ALLOWED_ORIGINS=https://wordle.example.com`: it listened on 9090, reported healthy, allowed that origin, rejected `localhost:3000`, and accepted `ADIEU`, `BOATS` and `TEAMS` as guesses. This includes `GameServiceTest` (`@SpringBootTest @Transactional`, rolling back and clearing the persistence context to force real database reads) and `GamePersistenceIntegrationTest` (non-transactional, so every service call commits and later reads come from committed rows; it deletes the games it creates).
- A real restart was verified end to end: the packaged jar was started, a game was created and guessed against over HTTP, the JVM was stopped, and a fresh JVM served `GET /api/games/{id}` with the earlier guess intact and accepted a further guess.

## Important current limitations

- The frontend's double-submit guard in `app/page.tsx` uses React state (`isSubmitting`), so two Enter presses within one render can both send a request. The backend rejects the second with a 409, but a ref-based guard would stop the UI from causing it.
- Finished games are never removed; the `games` table grows without bound.
- `pom.xml` targets Java 21, but the local runtime currently reports Java 26. This is non-blocking so far, but should be aligned to Java 21 later.

## Learning backlog

The user is treating this project as a learning exercise: concepts are explained before changes are made.

- **Testcontainers**: teach how it starts a throwaway PostgreSQL in Docker per test run, compared with the current second local database (`wordle_test`). It removes the manual database setup and the need for `@ActiveProfiles("test")` on every class. Deferred because Docker is out of scope for now.
- **Self-hosting on a DigitalOcean Droplet ($4/month)**: a later lesson in running your own server (Linux, Java service, HTTPS, firewall, updates) instead of Render's managed hosting. Config is already in environment variables, so moving is cheap; the database stays on Neon.
- **Frontend double-submit guard**: React state versus `useRef`, re-renders and stale closures, before changing `app/page.tsx`.

- `POST /api/games` has no rate limit; with no cleanup, a bot could fill a free-tier database. Fix soon after deploying.
- Tests only run locally; there is no CI yet.

## Exact next step

Deploy: create a managed PostgreSQL database, deploy the backend with the environment variables above, then deploy the frontend with `NEXT_PUBLIC_API_URL` pointing at the backend and add the frontend's URL to `WORDLE_CORS_ALLOWED_ORIGINS`. After that: CI, a rate limit on game creation, and finished-game cleanup.

## Running locally

Start PostgreSQL if needed:

```bash
brew services start postgresql@16
```

One-time setup for the test database (run as the Homebrew superuser):

```bash
psql -d postgres -c "CREATE DATABASE wordle_test OWNER wordle_app;"
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
