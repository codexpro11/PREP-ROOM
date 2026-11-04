package com.substring.docmind.interview;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PronunciationExercisesTest {
    @Test void firstExerciseIsImmediateAndRespectsCustomTextLimit() {
        var ordinary = PracticeOptions.normalize(Interview.Mode.PRONUNCIATION, null);
        assertThat(PronunciationExercises.first(ordinary)).contains("I will present my project today", "stressed syllable");

        var custom = PracticeOptions.normalize(Interview.Mode.PRONUNCIATION,
                new PracticeOptions(null, null, PracticeOptions.PronunciationFocus.INTONATION,
                        PracticeOptions.ExerciseFormat.CUSTOM, null, "word ".repeat(80)));
        assertThat(PronunciationExercises.first(custom).split("word", -1)).hasSize(61);
    }
}
