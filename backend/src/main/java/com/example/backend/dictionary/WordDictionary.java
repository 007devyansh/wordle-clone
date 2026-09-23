package com.example.backend.dictionary;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Holds two word lists, like the original Wordle: a short list of common words
 * that can be chosen as answers, and a much larger list of every word a player
 * may guess. Keeping them apart means answers stay fair (no obscure words or
 * plurals) while guesses stay forgiving (any real word is accepted).
 */
@Component
public class WordDictionary {

    private static final String ANSWERS_FILE = "dictionary/answers.txt";
    private static final String ALLOWED_GUESSES_FILE = "dictionary/allowed-guesses.txt";

    private final List<String> answers;
    private final Set<String> allowedGuesses;

    public WordDictionary() {
        this.answers = loadWords(ANSWERS_FILE);
        this.allowedGuesses = Set.copyOf(loadWords(ALLOWED_GUESSES_FILE));

        if (!allowedGuesses.containsAll(answers)) {
            throw new IllegalStateException("Every answer must also be an allowed guess.");
        }
    }

    public String randomAnswer() {
        int randomIndex = ThreadLocalRandom.current().nextInt(answers.size());
        return answers.get(randomIndex);
    }

    public boolean isAllowedGuess(String word) {
        return allowedGuesses.contains(word);
    }

    private static List<String> loadWords(String path) {
        ClassPathResource resource = new ClassPathResource(path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            List<String> words = reader.lines()
                    .map(String::trim)
                    .filter(word -> !word.isEmpty())
                    .toList();

            validateWords(path, words);

            return words;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the word list " + path + ".", exception);
        }
    }

    private static void validateWords(String path, List<String> words) {
        if (words.isEmpty() || words.stream().anyMatch(word -> !word.matches("[A-Z]{5}"))) {
            throw new IllegalStateException(
                    "The word list " + path + " must contain uppercase five-letter words."
            );
        }
    }
}
