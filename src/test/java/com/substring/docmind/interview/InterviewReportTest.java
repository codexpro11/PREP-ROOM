package com.substring.docmind.interview;

import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static com.substring.docmind.interview.InterviewDtos.*;
import static org.assertj.core.api.Assertions.*;

class InterviewReportTest {
    @Test void languageReportsIncludePracticeSettingsAndExcludeInterviewMetrics() throws Exception {
        var feedback = new Feedback(80, 82, null, null, "A clear response.", List.of("Specific words"),
                List.of("Pause between ideas"), List.of("Practise a short phrase"),
                "Are you ready?", "Not assessed: typed answers contain no audio evidence.");
        for (var mode : List.of(Interview.Mode.COMMUNICATION, Interview.Mode.PRONUNCIATION)) {
            var options = PracticeOptions.normalize(mode, new PracticeOptions(PracticeOptions.Scenario.PRESENTATION,
                    PracticeOptions.CommunicationGoal.STRUCTURE, PracticeOptions.PronunciationFocus.INTONATION,
                    PracticeOptions.ExerciseFormat.CUSTOM, "Travel", "Are you ready?"));
            var detail = new Detail(UUID.randomUUID(), options.title(mode), mode, Interview.Style.FRIENDLY,
                    Interview.Level.BEGINNER, Interview.Status.ANALYZED, null, null, 3, Instant.now(), Instant.now(),
                    List.of(), feedback, options);
            try (var pdf = Loader.loadPDF(new InterviewReport().create(detail))) {
                String text = new PDFTextStripper().getText(pdf);
                assertThat(text).contains("Travel", "Not assessed").doesNotContain("Company document:", "STAR:", "untrusted data");
                if (mode == Interview.Mode.PRONUNCIATION) {
                    assertThat(text).contains("Intonation", "My own text", "Are you ready?", "Exercise completion:").doesNotContain("Grammar:");
                } else assertThat(text).contains("Presenting an idea", "Organize an explanation");
            }
        }
    }
    @Test void reportWrapsLongAnswersAcrossPagesAndKeepsFeedback() throws Exception {
        var feedback = new Feedback(80, 82, null, 75, "Clear answer; include measurable results.", List.of("Specific actions"),
                List.of("Explain the result"), List.of("Practice a STAR answer", "Record an answer", "Review your wording"),
                "I improved [metric] by [amount].", "Not assessed: typed answers contain no audio evidence.");
        var transcript = new Turn(UUID.randomUUID(), "CANDIDATE", "A long answer with measurable results. ".repeat(220) + " END OF TRANSCRIPT", null, null, null, Instant.now());
        var detail = new Detail(UUID.randomUUID(), "Java Developer", Interview.Mode.INTERVIEW, Interview.Style.FRIENDLY,
                Interview.Level.INTERMEDIATE, Interview.Status.ANALYZED, null, null, 5, Instant.now(), Instant.now(), List.of(transcript), feedback, null);
        byte[] bytes = new InterviewReport().create(detail);
        try (var pdf = Loader.loadPDF(bytes)) {
            assertThat(pdf.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(pdf)).contains("END OF TRANSCRIPT", "Not assessed", "PRACTICE PLAN");
            Files.createDirectories(Path.of("target", "report-verification"));
            ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0, 100), "png", Path.of("target", "report-verification", "page-1.png").toFile());
        }
    }
}
