package com.substring.docmind.interview;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Validates the PCM WAV format emitted by the recorder without trusting client timing. */
public final class WavAudio {
    private WavAudio() {}
    public static double duration(byte[] bytes) {
        if (bytes.length < 44 || bytes.length > 12 * 1024 * 1024) throw invalid();
        var b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (!tag(bytes, 0, "RIFF") || !tag(bytes, 8, "WAVE") || !tag(bytes, 12, "fmt ") || !tag(bytes, 36, "data")
                || b.getInt(4) != bytes.length - 8 || b.getInt(16) != 16 || b.getShort(20) != 1 || b.getShort(22) != 1
                || b.getShort(32) != 2 || b.getShort(34) != 16 || b.getInt(40) != bytes.length - 44) throw invalid();
        int sampleRate = b.getInt(24);
        if (sampleRate < 16000 || sampleRate > 48000 || b.getInt(28) != sampleRate * 2 || (bytes.length - 44) % 2 != 0) throw invalid();
        double seconds = (bytes.length - 44) / (double) (sampleRate * 2);
        if (seconds < 0.5 || seconds > 180.1) throw invalid();
        return seconds;
    }
    private static boolean tag(byte[] bytes, int offset, String value) {
        return new String(bytes, offset, 4, StandardCharsets.US_ASCII).equals(value);
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Record 1 second to 3 minutes of audio using the app recorder (mono PCM WAV).");
    }
}
