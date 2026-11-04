// Capture uncompressed PCM so Gemini receives a portable WAV file in every supported browser.
class PcmRecorder extends AudioWorkletProcessor {
  process(inputs) {
    const samples = inputs[0]?.[0];
    if (samples) this.port.postMessage(samples.slice());
    return true;
  }
}
registerProcessor('pcm-recorder', PcmRecorder);
