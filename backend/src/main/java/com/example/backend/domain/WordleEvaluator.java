package com.example.backend.domain;

import java.util.*;

public class WordleEvaluator {

    public List<LetterResult> evaluate(String answer, String guess) {

        List<LetterResult> results = new ArrayList<>(
                Collections.nCopies(guess.length(), LetterResult.ABSENT)
        );

        Map<Character, Integer> remainingLetterCounts = new HashMap<>();

        for (int index = 0; index < guess.length(); index++) {
            char answerLetter = answer.charAt(index);
            char guessLetter = guess.charAt(index);

            if (guessLetter == answerLetter) {
                results.set(index, LetterResult.CORRECT);
            } else {
                remainingLetterCounts.merge(answerLetter, 1, Integer::sum);
            }
        }

        for (int index = 0; index < guess.length(); index++) {
            if (results.get(index) == LetterResult.CORRECT) {
                continue;
            }

            char guessLetter = guess.charAt(index);
            int remainingCount = remainingLetterCounts.getOrDefault(guessLetter, 0);

            if (remainingCount > 0) {
                results.set(index, LetterResult.PRESENT);
                remainingLetterCounts.put(guessLetter, remainingCount - 1);
            }
        }

        return List.copyOf(results);
    }
}
