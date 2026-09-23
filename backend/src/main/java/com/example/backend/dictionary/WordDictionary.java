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

@Component
public class WordDictionary {

    private final List<String> words;
    private final Set<String> wordSet;

    public WordDictionary() {
        this.words = loadWords();
        validateWords(words);
        this.wordSet = Set.copyOf(words);
    }

    public String randomWord() {
        int randomIndex = ThreadLocalRandom.current().nextInt(words.size());
        return words.get(randomIndex);
    }

    public boolean contains(String word) {
        return wordSet.contains(word);
    }

    private List<String> loadWords() {
        ClassPathResource resource = new ClassPathResource("words.txt");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(word -> !word.isEmpty())
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the word dictionary.", exception);
        }
    }

    private void validateWords(List<String> loadedWords) {
        if (loadedWords.isEmpty() || loadedWords.stream().anyMatch(word -> !word.matches("[A-Z]{5}"))) {
            throw new IllegalStateException("The word dictionary must contain uppercase five-letter words.");
        }
    }
}
