package com.example.backend.dictionary;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordDictionaryTest {

    private final WordDictionary dictionary = new WordDictionary();

    @Test
    void accepts_a_common_word_as_a_guess() {
        assertThat(dictionary.isAllowedGuess("CRANE")).isTrue();
    }

    @Test
    void accepts_a_valid_word_that_is_never_an_answer() {
        // A plural: real Wordle accepts it as a guess but never picks it as the answer.
        assertThat(dictionary.isAllowedGuess("BOATS")).isTrue();
    }

    @Test
    void rejects_a_made_up_word() {
        assertThat(dictionary.isAllowedGuess("QWERT")).isFalse();
    }

    @Test
    void every_chosen_answer_is_an_allowed_guess() {
        for (int i = 0; i < 100; i++) {
            assertThat(dictionary.isAllowedGuess(dictionary.randomAnswer())).isTrue();
        }
    }
}
