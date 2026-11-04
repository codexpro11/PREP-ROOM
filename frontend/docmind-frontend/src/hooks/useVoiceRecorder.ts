import { useCallback, useEffect, useRef, useState } from 'react';

export function useVoiceRecorder() {
  const [recording, setRecording] = useState(false);
  const [preparing, setPreparing] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const stream = useRef<MediaStream | null>(null);
  const context = useRef<AudioContext | null>(null);
  const analyser = useRef<AnalyserNode | null>(null);
  const chunks = useRef<Float32Array[]>([]);
  const node = useRef<AudioWorkletNode | null>(null);
  const interval = useRef<ReturnType<typeof setInterval> | null>(null);
  const rate = useRef(16000);
  const mounted = useRef(true);
  const starting = useRef(false);
  const samples = useRef(0);

  const release = useCallback(() => {
    stream.current?.getTracks().forEach(t => t.stop()); stream.current = null;
    node.current?.disconnect(); node.current = null;
    analyser.current?.disconnect(); analyser.current = null;
    void context.current?.close(); context.current = null;
    if (interval.current) clearInterval(interval.current);
    interval.current = null;
  }, []);
  useEffect(() => { mounted.current = true; return () => { mounted.current = false; release(); }; }, [release]);

  const start = useCallback(async (sound = true) => {
    if (starting.current || context.current) return;
    if (!navigator.mediaDevices?.getUserMedia) throw new Error('Microphone recording needs HTTPS or localhost and a supported browser. You can still type your answer.');
    starting.current = true;
    setPreparing(true);
    try {
      const audio = new AudioContext({ sampleRate: 16000 }); context.current = audio; rate.current = audio.sampleRate;
      await audio.resume();
      const media = await navigator.mediaDevices.getUserMedia({ audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true } });
      if (!mounted.current) { media.getTracks().forEach(t => t.stop()); return; }
      stream.current = media;
      await audio.audioWorklet.addModule('/pcm-recorder.js');
      if (!mounted.current) { release(); return; }
      // Finish the gentle two-note cue before connecting the microphone to capture.
      if (sound) {
        const began = audio.currentTime;
        [523.25, 783.99].forEach((frequency, i) => {
          const tone = audio.createOscillator(); const gain = audio.createGain();
          tone.frequency.value = frequency;
          const at = began + i * 0.09;
          gain.gain.setValueAtTime(0, at);
          gain.gain.linearRampToValueAtTime(0.055, at + 0.012);
          gain.gain.exponentialRampToValueAtTime(0.001, at + 0.12);
          tone.connect(gain); gain.connect(audio.destination);
          tone.start(at); tone.stop(at + 0.13);
          tone.onended = () => { tone.disconnect(); gain.disconnect(); };
        });
        await new Promise(resolve => setTimeout(resolve, 320));
        if (!mounted.current) { release(); return; }
      }
      const recorder = new AudioWorkletNode(audio, 'pcm-recorder'); node.current = recorder;
      chunks.current = []; samples.current = 0;
      recorder.port.onmessage = (event: MessageEvent<Float32Array>) => {
        if (samples.current < rate.current * 180) { chunks.current.push(event.data); samples.current += event.data.length; }
      };
      const silent = audio.createGain(); silent.gain.value = 0;
      const meter = audio.createAnalyser(); meter.fftSize = 256; meter.smoothingTimeConstant = 0.75; analyser.current = meter;
      audio.createMediaStreamSource(media).connect(meter); meter.connect(recorder); recorder.connect(silent); silent.connect(audio.destination);
      setSeconds(0); setRecording(true);
      const began = Date.now();
      interval.current = setInterval(() => setSeconds(Math.min(180, Math.floor((Date.now() - began) / 1000))), 250);
    } catch (e) { release(); throw e; }
    finally { starting.current = false; if (mounted.current) setPreparing(false); }
  }, [release]);

  const stop = useCallback((): Blob => {
    release(); setRecording(false);
    const length = chunks.current.reduce((sum, chunk) => sum + chunk.length, 0);
    const buffer = new ArrayBuffer(44 + length * 2); const view = new DataView(buffer);
    const write = (offset: number, text: string) => [...text].forEach((c, i) => view.setUint8(offset + i, c.charCodeAt(0)));
    write(0, 'RIFF'); view.setUint32(4, 36 + length * 2, true); write(8, 'WAVE'); write(12, 'fmt ');
    view.setUint32(16, 16, true); view.setUint16(20, 1, true); view.setUint16(22, 1, true);
    view.setUint32(24, rate.current, true); view.setUint32(28, rate.current * 2, true);
    view.setUint16(32, 2, true); view.setUint16(34, 16, true); write(36, 'data'); view.setUint32(40, length * 2, true);
    let offset = 44;
    for (const chunk of chunks.current) for (const sample of chunk) {
      const clamped = Math.max(-1, Math.min(1, sample)); view.setInt16(offset, clamped * (clamped < 0 ? 32768 : 32767), true); offset += 2;
    }
    chunks.current = [];
    return new Blob([buffer], { type: 'audio/wav' });
  }, [release]);
  const cancel = useCallback(() => { release(); chunks.current = []; setRecording(false); }, [release]);
  return { recording, preparing, analyser, seconds, start, stop, cancel };
}
