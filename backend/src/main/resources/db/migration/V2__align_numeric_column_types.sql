ALTER TABLE games
    ALTER COLUMN max_attempts TYPE INTEGER;

ALTER TABLE guesses
    ALTER COLUMN attempt_number TYPE INTEGER;

ALTER TABLE guess_results
    ALTER COLUMN letter_index TYPE INTEGER;
