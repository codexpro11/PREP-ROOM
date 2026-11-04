import { useEffect, useRef } from 'react';
import { Mic } from 'lucide-react';

const bars = Array.from({ length: 36 }, (_, i) => i);

export default function RecordingVisualizer({ analyser, seconds }: { analyser: AnalyserNode | null; seconds: number }) {
  const waveform = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const elements = waveform.current?.children;
    if (!analyser || !elements) return;
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const data = new Uint8Array(analyser.frequencyBinCount);
    let frame = 0;
    let last = 0;
    function draw(time: number) {
      if (!reducedMotion.matches && time - last > 45) {
        analyser!.getByteFrequencyData(data);
        for (let i = 0; i < elements!.length; i++) {
          const value = data[Math.floor(i * data.length / elements!.length)] / 255;
          (elements![i] as HTMLElement).style.transform = `scaleY(${0.08 + value * 0.92})`;
        }
        last = time;
      }
      frame = requestAnimationFrame(draw);
    }
    frame = requestAnimationFrame(draw);
    return () => cancelAnimationFrame(frame);
  }, [analyser]);

  return <div className="recording-stage">
    <div className="recording-stage-heading"><span className="recording-status" role="status"><span className="recording-dot" />LIVE RECORDING</span><span className="recording-timer" aria-label={`${seconds} seconds recorded`}>{Math.floor(seconds / 60)}:{String(seconds % 60).padStart(2, '0')}<span> / 3:00</span></span></div>
    <div className="recording-visual" aria-hidden="true"><div className="recording-mic"><Mic size={26} /></div><div className="recording-waveform" ref={waveform}>{bars.map(i => <span key={i} style={{ transform: `scaleY(${0.12 + Math.sin(i * 0.8) ** 2 * 0.23})` }} />)}</div></div>
    <div className="recording-caption"><strong>Your voice, in focus.</strong><span>Take your time. We’re listening.</span></div>
  </div>;
}
