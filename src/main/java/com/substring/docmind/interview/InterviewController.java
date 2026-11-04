package com.substring.docmind.interview;

import com.substring.docmind.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.*;
import static com.substring.docmind.interview.InterviewDtos.*;

@RestController @RequestMapping("/api/v1/interviews") @RequiredArgsConstructor
public class InterviewController {
    private final InterviewService service;
    private final InterviewReport report;
    @GetMapping public List<Summary> list(Authentication auth) { return service.list((User) auth.getPrincipal()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Detail create(@Valid @RequestBody Setup setup, Authentication auth) { return service.create(setup, (User) auth.getPrincipal()); }
    @GetMapping("/{id}") public Detail get(@PathVariable UUID id, Authentication auth) { return service.get(id, (User) auth.getPrincipal()); }
    @PostMapping("/{id}/answers") public Detail answer(@PathVariable UUID id, @Valid @RequestBody Answer answer, Authentication auth) {
        return service.answer(id, (User) auth.getPrincipal(), answer);
    }
    @PostMapping(value = "/{id}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Detail audio(@PathVariable UUID id, @RequestParam MultipartFile file, @RequestParam UUID requestId, Authentication auth) throws IOException {
        service.get(id, (User) auth.getPrincipal());
        if (file.isEmpty() || file.getSize() > 12 * 1024 * 1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Record between 1 second and 3 minutes (maximum 12 MB).");
        // Browser records PCM WAV; verify the container before sending it to Gemini.
        byte[] bytes = file.getBytes();
        WavAudio.duration(bytes);
        if (bytes.length < 44 || bytes[0] != 'R' || bytes[1] != 'I' || bytes[2] != 'F' || bytes[3] != 'F'
                || bytes[8] != 'W' || bytes[9] != 'A' || bytes[10] != 'V' || bytes[11] != 'E') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload a WAV audio recording.");
        }
        return service.audio(id, (User) auth.getPrincipal(), bytes, "audio/wav", requestId);
    }
    @PostMapping("/{id}/finish") public Detail finish(@PathVariable UUID id, Authentication auth) { return service.finish(id, (User) auth.getPrincipal()); }
    @GetMapping(value = "/{id}/report", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> report(@PathVariable UUID id, Authentication auth) throws IOException {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=preproom-" + id + ".pdf")
                .cacheControl(CacheControl.noStore()).body(report.create(service.get(id, (User) auth.getPrincipal())));
    }
}
