package com.substring.docmind.interview;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.util.*;
import static com.substring.docmind.interview.InterviewDtos.*;

@Service
public class InterviewReport {
    public byte[] create(Detail session) throws IOException {
        if (session.feedback() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Generate feedback before downloading a report.");
        List<String> lines = new ArrayList<>();
        lines.add("PREPROOM | Practice report");
        lines.add(session.jobRole() + " / " + session.mode() + " / " + session.level());
        lines.add("Date: " + session.createdAt());
        if (session.mode() == Interview.Mode.INTERVIEW) {
            lines.add("Company document: " + Objects.toString(session.documentName(), "None"));
        } else {
            lines.add(PracticeOptions.normalize(session.mode(), session.practiceOptions()).description(session.mode()));
        }
        lines.add("AI coaching estimates; not a hiring prediction or standardized language assessment.");
        var f = session.feedback();
        lines.add(session.mode() == Interview.Mode.PRONUNCIATION ? "Exercise completion: " + f.relevanceScore() + "/100"
                : "Relevance: " + f.relevanceScore() + "/100 | Grammar: " + f.grammarScore() + "/100");
        lines.add("Fluency: " + Objects.toString(f.fluencyScore(), "Not assessed")
                + (session.mode() == Interview.Mode.INTERVIEW ? " | STAR: " + Objects.toString(f.starScore(), "Not applicable") : ""));
        lines.add("SUMMARY"); lines.add(f.summary());
        lines.add("STRENGTHS"); lines.addAll(f.strengths());
        lines.add("FOCUS NEXT"); lines.addAll(f.improvements());
        lines.add("PRACTICE PLAN"); lines.addAll(f.practicePlan());
        lines.add("EXAMPLE ANSWER"); lines.add(f.exampleAnswer());
        lines.add("PRONUNCIATION"); lines.add(f.pronunciationNotes());
        lines.add("TRANSCRIPT");
        session.transcripts().forEach(t -> {
            lines.add(t.speaker() + ": " + t.content());
            if (t.audioFeedback() != null) lines.add("Audio coaching: " + t.audioFeedback());
        });
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            List<String> wrapped = new ArrayList<>();
            for (String line : lines) {
                // Standard PDF font fallback; preserve supported characters and substitute unsupported glyphs.
                StringBuilder safe = new StringBuilder();
                for (int cp : line.codePoints().toArray()) {
                    String character = new String(Character.toChars(cp));
                    if (Character.isWhitespace(cp)) { safe.append(' '); continue; }
                    try { font.encode(character); safe.append(character); } catch (IllegalArgumentException e) { safe.append('?'); }
                }
                String current = "";
                for (String word : safe.toString().split("\\s+")) {
                    // Split long tokens too, so URLs and long identifiers never run off the page.
                    for (int start = 0; start < word.length(); start += 65) {
                        String part = word.substring(start, Math.min(start + 65, word.length()));
                        String next = current.isEmpty() ? part : current + " " + part;
                        if (font.getStringWidth(next) / 1000 * 11 > 500) { wrapped.add(current); current = part; }
                        else current = next;
                    }
                }
                wrapped.add(current); wrapped.add("");
            }
            for (int offset = 0; offset < wrapped.size(); offset += 44) {
                var page = new PDPage(); document.addPage(page);
                try (var stream = new PDPageContentStream(document, page)) {
                    stream.beginText(); stream.setFont(font, 11); stream.setLeading(16); stream.newLineAtOffset(50, 740);
                    for (int n = offset; n < Math.min(offset + 44, wrapped.size()); n++) { stream.showText(wrapped.get(n)); stream.newLine(); }
                    stream.endText();
                }
            }
            document.save(output); return output.toByteArray();
        }
    }
}
