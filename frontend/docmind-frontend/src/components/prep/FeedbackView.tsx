import { useState } from 'react';
import { ArrowRight, Check, Download, Sparkles, Target } from 'lucide-react';
import { Link } from 'react-router-dom';
import { errorMessage, interviewApi, type Interview } from '../../services/interviewApi';

export default function FeedbackView({ session }: { session: Interview }) {
  const [busy, setBusy] = useState(false); const [error, setError] = useState('');
  const f = session.feedback; if (!f) return null;
  async function download() { setBusy(true); setError(''); try { await interviewApi.report(session.id); } catch (e) { setError(errorMessage(e)); } finally { setBusy(false); } }
  const scores: [string, number | null][] = session.mode === 'PRONUNCIATION'
    ? [['Exercise completion', f.relevanceScore], ['Spoken fluency', f.fluencyScore]]
    : [['Answer relevance', f.relevanceScore], ['Grammar', f.grammarScore], ['Spoken fluency', f.fluencyScore],
      ...(session.mode === 'INTERVIEW' ? [['STAR structure', f.starScore] as [string, number | null]] : [])];
  return <section className="feedback-view"><div className="feedback-heading"><span className="card-icon green"><Sparkles /></span><div><div className="eyebrow">TURN PRACTICE INTO PROGRESS</div><h2>Here’s what you can build on.</h2></div><button className="secondary-button" disabled={busy} onClick={() => void download()}><Download size={16} />{busy ? 'Preparing…' : 'Download report'}</button></div><p className="feedback-summary">{f.summary}</p>
    <div className="score-grid">{scores.map(([label, value]) => <div className="score-card" key={label}><span>{label}</span><strong>{value == null ? '—' : value}<small>{value == null ? 'Not assessed' : '/100'}</small></strong><div className="score-track"><span style={{ width: `${typeof value === 'number' ? value : 0}%` }} /></div></div>)}</div><p className="score-disclaimer">AI coaching estimates, not a hiring prediction or a standardized language assessment. A dash means there was not enough applicable evidence.</p>
    <div className="feedback-columns"><article className="surface-panel"><h3><Check size={18} />What’s working</h3><ul>{f.strengths.map((text, i) => <li key={i}>{text}</li>)}</ul></article><article className="surface-panel"><h3><Target size={18} />Your next focus</h3><ul>{f.improvements.map((text, i) => <li key={i}>{text}</li>)}</ul></article></div>
    <article className="surface-panel"><h3>Your practice plan</h3><ol className="practice-plan">{f.practicePlan.map((text, i) => <li key={i}><span>{i + 1}</span>{text}</li>)}</ol></article><article className="surface-panel"><h3>A stronger way to say it</h3><p className="example-answer">{f.exampleAnswer}</p></article><article className="surface-panel"><h3>Pronunciation & delivery</h3><p>{f.pronunciationNotes}</p></article>{error && <div className="error-box" role="alert">{error}</div>}<Link to="/practice" className="primary-button">Put it into practice <ArrowRight size={17} /></Link>
  </section>;
}
