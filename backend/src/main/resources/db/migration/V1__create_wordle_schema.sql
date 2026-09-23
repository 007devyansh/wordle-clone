CREATE TABLE games (
    id UUID PRIMARY KEY,
    answer VARCHAR(5) NOT NULL,
    max_attempts SMALLINT NOT NULL CHECK (max_attempts > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('IN_PROGRESS', 'WON', 'LOST')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE guesses (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    attempt_number SMALLINT NOT NULL CHECK (attempt_number BETWEEN 1 AND 6),
    word VARCHAR(5) NOT NULL CHECK (word ~ '^[A-Z]{5}$'),
    CONSTRAINT guesses_game_id_attempt_number_key UNIQUE (game_id, attempt_number)
);

CREATE TABLE guess_results (
    guess_id UUID NOT NULL REFERENCES guesses (id) ON DELETE CASCADE,
    letter_index SMALLINT NOT NULL CHECK (letter_index BETWEEN 0 AND 4),
    letter_result VARCHAR(10) NOT NULL CHECK (letter_result IN ('CORRECT', 'PRESENT', 'ABSENT')),
    PRIMARY KEY (guess_id, letter_index)
);
