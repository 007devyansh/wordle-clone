package com.example.backend.dictionary;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordDictionaryTest {

    private final WordDictionary dictionary = new WordDictionary();

    @Test
    void contains_a_known_word() {
        assertThat(dictionary.contains("CRANE")).isTrue();
    }

    @Test
    void does_not_contain_an_unknown_word() {
        assertThat(dictionary.contains("QWERT")).isFalse();
    }

    @Test
    void chooses_an_answer_from_the_dictionary() {
        assertThat(dictionary.contains(dictionary.randomWord())).isTrue();
    }
}
