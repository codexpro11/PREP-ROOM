package com.substring.docmind.interview;

import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Mode-specific settings. Missing settings support older clients and saved sessions. */
public record PracticeOptions(Scenario scenario, CommunicationGoal communicationGoal,
                              PronunciationFocus pronunciationFocus, ExerciseFormat exerciseFormat,
                              @Size(max = 200) String topic, @Size(max = 1200) String customText) {
    public enum Scenario {
        EVERYDAY("Everyday conversation"), WORKPLACE("Workplace conversation"),
        PRESENTATION("Presenting an idea"), DISCUSSION("Discussion & debate"),
        CUSTOMER("Customer conversation");
        public final String label;
        Scenario(String label) { this.label = label; }
    }
    public enum CommunicationGoal {
        CLARITY("Express ideas clearly"), FLUENCY("Keep a conversation flowing"),
        STRUCTURE("Organize an explanation"), VOCABULARY("Use precise vocabulary"),
        LISTENING("Respond & ask follow-up questions");
        public final String label;
        CommunicationGoal(String label) { this.label = label; }
    }
    public enum PronunciationFocus {
        SOUNDS("Individual sounds"), WORD_STRESS("Word stress"),
        SENTENCE_STRESS("Sentence stress & rhythm"), INTONATION("Intonation"),
        CONNECTED_SPEECH("Linking & connected speech");
        public final String label;
        PronunciationFocus(String label) { this.label = label; }
    }
    public enum ExerciseFormat {
        WORDS("Words & short phrases"), SENTENCES("Short sentences"),
        PASSAGE("A short passage"), CUSTOM("My own text");
        public final String label;
        ExerciseFormat(String label) { this.label = label; }
    }

    public static PracticeOptions normalize(Interview.Mode mode, PracticeOptions input) {
        if (mode == Interview.Mode.INTERVIEW) return null;
        var p = input == null ? new PracticeOptions(null, null, null, null, null, null) : input;
        String topic = trim(p.topic());
        if (mode == Interview.Mode.COMMUNICATION) {
            return new PracticeOptions(p.scenario() == null ? Scenario.EVERYDAY : p.scenario(),
                    p.communicationGoal() == null ? CommunicationGoal.CLARITY : p.communicationGoal(),
                    null, null, topic, null);
        }
        var focus = p.pronunciationFocus() == null ? PronunciationFocus.WORD_STRESS : p.pronunciationFocus();
        var format = p.exerciseFormat() == null ? ExerciseFormat.SENTENCES : p.exerciseFormat();
        String text = format == ExerciseFormat.CUSTOM ? trim(p.customText()) : null;
        if (format == ExerciseFormat.CUSTOM && text == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add the text you want to read aloud.");
        }
        if (format == ExerciseFormat.WORDS && focus != PronunciationFocus.SOUNDS && focus != PronunciationFocus.WORD_STRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose sentences or a passage to practice rhythm, intonation, or connected speech.");
        }
        return new PracticeOptions(null, null, focus, format, topic, text);
    }

    public String title(Interview.Mode mode) {
        return mode == Interview.Mode.COMMUNICATION ? scenario.label : pronunciationFocus.label;
    }

    public String description(Interview.Mode mode) {
        String detail = mode == Interview.Mode.COMMUNICATION
                ? "Situation: " + scenario.label + "\nSpeaking goal: " + communicationGoal.label
                : "Pronunciation focus: " + pronunciationFocus.label + "\nExercise format: " + exerciseFormat.label;
        return detail + (topic == null ? "" : "\nPractice topic / target words: " + topic)
                + (customText == null ? "" : "\nRead-aloud text: " + customText);
    }

    private static String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
