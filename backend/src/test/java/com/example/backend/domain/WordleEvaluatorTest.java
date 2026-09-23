package com.example.backend.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WordleEvaluatorTest {

    private final WordleEvaluator evaluator = new WordleEvaluator();

    @Test
    void marks_every_letter_correct_for_an_exact_match() {
        List<LetterResult> result = evaluator.evaluate("CRANE", "CRANE");

        assertThat(result).containsExactly(
                LetterResult.CORRECT,
                LetterResult.CORRECT,
                LetterResult.CORRECT,
                LetterResult.CORRECT,
                LetterResult.CORRECT
        );
    }

    @Test
    void marks_correct_present_and_absent_letters() {
        List<LetterResult> result = evaluator.evaluate("CRANE", "SLATE");

        assertThat(result).containsExactly(
                LetterResult.ABSENT,
                LetterResult.ABSENT,
                LetterResult.CORRECT,
                LetterResult.ABSENT,
                LetterResult.CORRECT
        );
    }

    @Test
    void does_not_mark_more_duplicate_letters_than_the_answer_contains() {
        List<LetterResult> result = evaluator.evaluate("EERIE", "ERROR");

        assertThat(result).containsExactly(
                LetterResult.CORRECT,
                LetterResult.ABSENT,
                LetterResult.CORRECT,
                LetterResult.ABSENT,
                LetterResult.ABSENT
        );
    }
}
