package com.substring.docmind.interview;

import org.junit.jupiter.api.Test;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class WavAudioTest {
    static byte[] recording(int seconds) {
        byte[] audio = new byte[44 + seconds * 32000]; var b = ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(audio.length - 8).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII));
        b.putInt(16).putShort((short) 1).putShort((short) 1).putInt(16000).putInt(32000).putShort((short) 2).putShort((short) 16);
        b.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(audio.length - 44); return audio;
    }
    @Test void derivesDurationFromPcmSamples() { assertThat(WavAudio.duration(recording(5))).isEqualTo(5); }
    @Test void rejectsOversizedDurationOrSpoofedHeader() {
        assertThatThrownBy(() -> WavAudio.duration(recording(181))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        byte[] bytes = recording(1); bytes[24] = 0;
        assertThatThrownBy(() -> WavAudio.duration(bytes)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> WavAudio.duration(new byte[44])).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
