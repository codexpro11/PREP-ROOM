package com.substring.docmind.interview;

import java.util.Arrays;
import java.util.stream.Collectors;

final class PronunciationExercises {
    private PronunciationExercises() {}

    static String first(PracticeOptions options) {
        if (options.exerciseFormat() == PracticeOptions.ExerciseFormat.CUSTOM) {
            String excerpt = Arrays.stream(options.customText().split("\\s+"))
                    .limit(60).collect(Collectors.joining(" "));
            return "Record yourself reading this aloud:\n\n“" + excerpt + "”\n\n" + tip(options);
        }
        String phrase = options.topic() == null ? switch (options.pronunciationFocus()) {
            case SOUNDS -> options.exerciseFormat() == PracticeOptions.ExerciseFormat.WORDS
                    ? "think, this, river, light" : "Three thin threads are on the table.";
            case WORD_STRESS -> options.exerciseFormat() == PracticeOptions.ExerciseFormat.WORDS
                    ? "present, relax, comfort, begin" : "I will present my project today.";
            case SENTENCE_STRESS -> "I ordered the blue notebook, not the red one.";
            case INTONATION -> "Are you ready to start? I am ready to start.";
            case CONNECTED_SPEECH -> "I want to ask about it. Could you help me?";
        } : options.topic();
        return "Record yourself reading this aloud:\n\n“" + phrase + "”\n\n" + tip(options);
    }

    private static String tip(PracticeOptions options) {
        return switch (options.pronunciationFocus()) {
            case SOUNDS -> "Tip: slow down and make each target sound distinct.";
            case WORD_STRESS -> "Tip: make the stressed syllable longer and clearer.";
            case SENTENCE_STRESS -> "Tip: emphasize the word that carries the main meaning.";
            case INTONATION -> "Tip: notice how your pitch changes at the end.";
            case CONNECTED_SPEECH -> "Tip: connect the final sound of one word to the next word.";
        };
    }
}
