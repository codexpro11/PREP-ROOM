import ReactMarkdown from 'react-markdown';
import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { AudioLines, ArrowLeft, Check, FileText, LoaderCircle, Mic, Send, Square, Volume2, VolumeX } from 'lucide-react';
import { interviewApi, errorMessage, type Interview } from '../../services/interviewApi';
import { useVoiceRecorder } from '../../hooks/useVoiceRecorder';
import { modeLabels, statusLabels } from './labels';
import { englishLevels, practiceDetails, styleLabels } from './practiceOptions';
import FeedbackView from './FeedbackView';
import RecordingVisualizer from './RecordingVisualizer';
import { usePreferences } from '../../store/preferencesStore';

function speak(text: string) {
  if (!('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel(); const utterance = new SpeechSynthesisUtterance(text.replace(/[*#`]/g, '')); utterance.lang = 'en-US'; utterance.rate = 0.95; window.speechSynthesis.speak(utterance);
}
export default function PracticeSession() {
  const { id } = useParams();
  const [session, setSession] = useState<Interview | null>(null); const [answer, setAnswer] = useState('');
  const [loading, setLoading] = useState(true); const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  const [autoSpeak, setAutoSpeak] = useState(false); const [pendingAudio, setPendingAudio] = useState<Blob | null>(null);
  const [retryLoad, setRetryLoad] = useState(0); const [confirmEnd, setConfirmEnd] = useState(false);
  const request = useRef({ id: crypto.randomUUID(), text: '' }); const audioId = useRef(crypto.randomUUID());
  const lastSpoken = useRef(''); const bottom = useRef<HTMLDivElement>(null); const working = useRef(false);
  const voice = useVoiceRecorder();
  const sound = usePreferences(s => s.sound);
  useEffect(() => { let active = true; interviewApi.get(id!).then(s => { if (active) setSession(s); }).catch(e => { if (active) setError(errorMessage(e)); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; window.speechSynthesis?.cancel(); }; }, [id, retryLoad]);
  useEffect(() => {
    if (session?.status !== 'ANALYZING') return;
    let active = true;
    const timer = setInterval(() => { interviewApi.get(id!).then(s => { if (active) { setSession(s); setError(''); } }).catch(e => { if (active) setError(errorMessage(e)); }); }, 4000);
    return () => { active = false; clearInterval(timer); };
  }, [id, session?.status]);
  const latest = session?.transcripts.filter(t => t.speaker === 'AI').at(-1);
  useEffect(() => { if (autoSpeak && latest && lastSpoken.current !== latest.id && !voice.recording && !voice.preparing && !busy) { speak(latest.content); lastSpoken.current = latest.id; } }, [autoSpeak, latest, voice.recording, voice.preparing, busy]);
  useEffect(() => { bottom.current?.scrollIntoView({ behavior: 'smooth', block: 'end' }); }, [session?.transcripts.length, busy]);
  useEffect(() => {
    const warn = (event: BeforeUnloadEvent) => { if (voice.recording || busy || answer.trim() || pendingAudio) event.preventDefault(); };
    window.addEventListener('beforeunload', warn); return () => window.removeEventListener('beforeunload', warn);
  }, [voice.recording, busy, answer, pendingAudio]);
  async function submitAudio(blob: Blob) {
    if (working.current) return; working.current = true; setBusy(true); setError('');
    try { setSession(await interviewApi.audio(id!, blob, audioId.current)); setPendingAudio(null); audioId.current = crypto.randomUUID(); }
    catch (e) { setPendingAudio(blob); setError(errorMessage(e)); } finally { working.current = false; setBusy(false); }
  }
  async function stopRecording() { const blob = voice.stop(); setPendingAudio(blob); await submitAudio(blob); }
  const stopRef = useRef(stopRecording); useEffect(() => { stopRef.current = stopRecording; });
  useEffect(() => { if (voice.recording && voice.seconds >= 180) void stopRef.current(); }, [voice.recording, voice.seconds]);
  async function startRecording() {
    if (working.current) return; working.current = true; setError(''); window.speechSynthesis?.cancel();
    try { await voice.start(sound); } catch (e) { setError(e instanceof Error ? e.message : 'Microphone access was denied. You can type your answer instead.'); } finally { working.current = false; }
  }
  async function send(e: React.FormEvent) {
    e.preventDefault(); if (working.current || !answer.trim()) return;
    working.current = true; setBusy(true); setError(''); window.speechSynthesis?.cancel();
    if (request.current.text !== answer) request.current = { id: crypto.randomUUID(), text: answer };
    try { setSession(await interviewApi.answer(id!, answer.trim(), request.current.id)); setAnswer(''); request.current = { id: crypto.randomUUID(), text: '' }; }
    catch (e) { setError(errorMessage(e)); } finally { working.current = false; setBusy(false); }
  }
  async function finish() {
    if (working.current) return; working.current = true; setBusy(true); setError(''); window.speechSynthesis?.cancel();
    try { setSession(await interviewApi.finish(id!)); setConfirmEnd(false); } catch (e) { setError(errorMessage(e)); } finally { working.current = false; setBusy(false); }
  }
  if (loading) return <div className="empty-state"><LoaderCircle className="spin" />Opening your practice room…</div>;
  if (!session) return <div className="error-box" role="alert">{error}<button onClick={() => { setLoading(true); setError(''); setRetryLoad(n => n + 1); }}>Try again</button><Link to="/">Back to overview</Link></div>;
  const answered = session.transcripts.filter(t => t.speaker === 'CANDIDATE').length;
  const active = session.status === 'IN_PROGRESS';
  return <><Link to="/history" className="back-link"><ArrowLeft size={16} />All sessions</Link><div className="page-heading session-heading"><div><div className="eyebrow">{modeLabels[session.mode]} · {styleLabels[session.mode][session.style]}</div><h1>{session.jobRole}</h1><p>{session.documentName ? <><FileText size={14} />Grounded in {session.documentName}</> : 'Your space to practice, explore, and improve.'}</p></div><span className="session-badge done">{statusLabels[session.status]}</span></div>
    {session.mode !== 'INTERVIEW' && <div className="session-practice-details">{practiceDetails(session.mode, session.practiceOptions).map(([label, value]) => <span key={label}><strong>{label}:</strong> {value}</span>)}<span><strong>English level:</strong> {englishLevels[session.level]}</span></div>}
    {session.feedback && <FeedbackView session={session} />}
    <div className="room-layout"><section className="conversation-panel"><div className="conversation-top"><span><span className="status-dot" />PrepRoom coach</span><span>{answered} / {session.questionCount} answers</span></div><div className="progress-track"><span style={{ width: `${answered / session.questionCount * 100}%` }} /></div><div className="transcript" aria-live="polite">{session.transcripts.map(t => <article key={t.id} className={`turn ${t.speaker === 'AI' ? 'coach-turn' : 'candidate-turn'}`}><div className="turn-speaker"><span className="turn-avatar">{t.speaker === 'AI' ? <AudioLines size={17} /> : 'You'}</span><strong>{t.speaker === 'AI' ? 'Your coach' : 'You'}</strong>{t.audioSeconds != null && <span className="turn-type"><Mic size={12} />Recorded answer</span>}{t.speaker === 'AI' && 'speechSynthesis' in window && <button type="button" disabled={voice.recording || voice.preparing} onClick={() => speak(t.content)} aria-label="Read this question aloud"><Volume2 size={16} /></button>}</div><div className="coach-message">{t.speaker === 'AI' ? <ReactMarkdown>{t.content}</ReactMarkdown> : <p>{t.content}</p>}</div>{t.sources && <span className="source-label"><FileText size={12} />{t.sources}</span>}{t.audioFeedback && <details className="audio-notes"><summary>Speech coaching · AI estimate</summary><p>{t.audioFeedback}</p></details>}</article>)}{busy && <div className="coach-thinking"><LoaderCircle className="spin" size={16} />{pendingAudio ? 'Listening to your answer and preparing a response…' : 'Your coach is preparing…'}</div>}<div ref={bottom} /></div>
    {error && <div className="error-box" role="alert">{error}</div>}
    {active && <div className="answer-panel">{pendingAudio && !busy && <div className="recording-retry"><p>Your recording is ready to retry. Retrying won’t add a duplicate answer.</p><button className="secondary-button" onClick={() => void submitAudio(pendingAudio)}>Retry recording</button><button onClick={() => { setPendingAudio(null); audioId.current = crypto.randomUUID(); }}>Discard</button></div>}{voice.recording ? <div className="recording-card"><RecordingVisualizer analyser={voice.analyser.current} seconds={voice.seconds} /><div className="recording-bar"><small>Speak naturally. Up to 3 minutes per answer.</small><button className="primary-button" disabled={voice.seconds < 1} onClick={() => void stopRecording()}><Square size={14} />Stop & send</button><button onClick={voice.cancel}>Cancel</button></div></div> : <form onSubmit={send}><label htmlFor="answer" className="sr-only">Your answer</label><textarea id="answer" value={answer} maxLength={8000} onChange={e => setAnswer(e.target.value)} disabled={busy || voice.preparing || !!pendingAudio} placeholder={session.mode === 'PRONUNCIATION' ? 'Record your voice for pronunciation coaching, or type a question for your coach…' : 'Take a breath. Answer in your own words…'} rows={3} /><div className="answer-actions"><button type="button" className="secondary-button" disabled={busy || voice.preparing || !!pendingAudio} onClick={() => void startRecording()}>{voice.preparing ? <><LoaderCircle className="spin" size={16} />Getting mic ready…</> : <><Mic size={16} />Record answer</>}</button><span>{answer.length}/8000</span><button className="primary-button" disabled={busy || voice.preparing || !!pendingAudio || !answer.trim()}><Send size={16} />Send answer</button></div></form>}<small className="voice-privacy">Recording starts only when you choose. Audio is sent to Gemini for transcription and coaching, then discarded by this app; transcript and coaching notes are saved.</small></div>}
    {!active && !session.feedback && <div className="session-complete"><Check size={24} /><h3>{session.status === 'ANALYZING' ? 'Finding the useful takeaways…' : 'You showed up. That’s progress.'}</h3><p>{session.status === 'ANALYZING' ? 'Your feedback is being prepared. You can leave and return to this saved session.' : session.status === 'ANALYSIS_FAILED' ? 'Feedback couldn’t be generated. Your answers are saved — try again.' : 'Let’s turn your answers into a practical plan for your next session.'}</p><button className="primary-button" disabled={busy} onClick={() => void finish()}>{session.status === 'ANALYZING' ? 'Check / retry after 5 minutes' : 'Generate my feedback'}</button></div>}
    </section><aside className="room-aside"><div className="surface-panel"><div className="summary-orb"><AudioLines size={38} /></div><h3>One question at a time.</h3><p>You don’t have to be perfect. Take a moment to think, then let your answer flow.</p>{'speechSynthesis' in window ? <button className={`speech-toggle ${autoSpeak ? 'enabled' : ''}`} aria-pressed={autoSpeak} onClick={() => { setAutoSpeak(v => !v); if (autoSpeak) window.speechSynthesis.cancel(); }}>{autoSpeak ? <Volume2 size={18} /> : <VolumeX size={18} />}Read questions aloud <span>{autoSpeak ? 'On' : 'Off'}</span></button> : <p>Speech playback is unavailable in this browser. Questions are always shown as text.</p>}</div><div className="coaching-tip"><span className="eyebrow">A LITTLE REMINDER</span><h3>{session.mode === 'INTERVIEW' ? 'Give your answer a story.' : 'Clarity comes before speed.'}</h3><p>{session.mode === 'INTERVIEW' ? 'Situation. Task. Action. Result. A concrete example makes your experience easier to understand.' : 'Pause between ideas, stress the important words, and aim to be understood. Your accent is part of your voice.'}</p></div>{active && <><button className="secondary-button end-button" disabled={busy || voice.recording || voice.preparing || !!pendingAudio || answered === 0} onClick={() => setConfirmEnd(true)}>Finish & get feedback</button>{confirmEnd && <div className="surface-panel"><p>Finish now with {answered} saved answers? {answer.trim() ? 'Your unsent draft will not be included.' : 'You can review the transcript afterward.'}</p><button className="primary-button" disabled={busy} onClick={() => void finish()}>Finish session</button><button className="text-link" onClick={() => setConfirmEnd(false)}>Keep practicing</button></div>}<small>Your answers are saved after each completed turn. Come back anytime to continue.</small></>}</aside></div>
  </>;
}



