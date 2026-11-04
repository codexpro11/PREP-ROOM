import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowRight, AudioLines, BriefcaseBusiness, FileText, LoaderCircle, MessageCircle } from 'lucide-react';
import { documentApi } from '../../services/api';
import { errorMessage, interviewApi, type Mode, type PracticeOptions, type Setup } from '../../services/interviewApi';
import type { DocumentMetadataDto } from '../../types';
import { modeLabels } from './labels';
import { communicationGoals, defaultSetups, englishLevels, exerciseFormats, practiceDetails, pronunciationFocuses, scenarios, styleLabels } from './practiceOptions';

const roles = ['Software Engineer', 'Java / Spring Boot Developer', 'Frontend Developer', 'Data Analyst', 'Data Scientist', 'Product Manager', 'UX Designer', 'DevOps Engineer', 'Business Analyst', 'Sales & Marketing', 'Customer Support', 'Other'];
const modeChoices = [
  { mode: 'INTERVIEW', icon: BriefcaseBusiness, desc: 'Prepare for your next role' },
  { mode: 'COMMUNICATION', icon: MessageCircle, desc: 'Build fluency and expression' },
  { mode: 'PRONUNCIATION', icon: AudioLines, desc: 'Practice clarity and word stress' },
] as const;
function options(labels: Record<string, string>) {
  return Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>);
}

export default function PracticeSetup() {
  const [params, setParams] = useSearchParams(); const navigate = useNavigate();
  const requestedMode = params.get('mode');
  const mode: Mode = modeChoices.find(c => c.mode === requestedMode)?.mode ?? 'INTERVIEW';
  const [drafts, setDrafts] = useState(defaultSetups);
  const [role, setRole] = useState(roles[1]); const [documents, setDocuments] = useState<DocumentMetadataDto[]>([]);
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [docError, setDocError] = useState('');
  const submitting = useRef(false);
  const setup = drafts[mode]; const practice = setup.practiceOptions;
  const interview = mode === 'INTERVIEW'; const pronunciation = mode === 'PRONUNCIATION';
  useEffect(() => {
    let active = true;
    if (interview) documentApi.getMine().then(r => {
      if (active) { setDocuments(r.data.filter(d => d.status === 'INDEXED')); setDocError(''); }
    }).catch(() => { if (active) setDocError('Your company library could not be loaded. You can still practice without a document.'); });
    return () => { active = false; };
  }, [interview]);
  const update = <K extends keyof Setup>(key: K, value: Setup[K]) =>
    setDrafts(d => ({ ...d, [mode]: { ...d[mode], [key]: value } }));
  function updatePractice<K extends keyof PracticeOptions>(key: K, value: PracticeOptions[K]) {
    setDrafts(d => {
      const next = { ...d[mode].practiceOptions, [key]: value };
      if (key === 'pronunciationFocus' && next.exerciseFormat === 'WORDS' && value !== 'SOUNDS' && value !== 'WORD_STRESS') {
        next.exerciseFormat = 'SENTENCES';
      }
      return { ...d, [mode]: { ...d[mode], practiceOptions: next } };
    });
  }
  const customText = pronunciation && practice?.exerciseFormat === 'CUSTOM';
  const valid = interview ? !!setup.jobRole?.trim() : !customText || !!practice?.customText?.trim();
  const units = interview ? 'questions' : pronunciation ? 'exercises' : 'conversation turns';
  const details = practiceDetails(mode, practice);
  async function start(e: React.FormEvent) {
    e.preventDefault(); if (submitting.current || !valid) return;
    submitting.current = true; setBusy(true); setError('');
    // Only serialize settings for the active mode; switching keeps each draft in memory.
    const payload: Setup = interview ? { ...setup, practiceOptions: undefined } : {
      mode, style: setup.style, level: setup.level, questionCount: setup.questionCount,
      practiceOptions: { ...practice, customText: customText ? practice?.customText?.trim() : undefined, topic: practice?.topic?.trim() || undefined },
    };
    try { const session = await interviewApi.create(payload); navigate(`/session/${session.id}`); }
    catch (e) { setError(errorMessage(e)); } finally { submitting.current = false; setBusy(false); }
  }
  return <>
    <div className="page-heading"><div><div className="eyebrow">A SESSION THAT FITS YOU</div><h1>Let’s make it your kind of practice.</h1><p>Choose a focus, set the pace, and we’ll take it from there.</p></div></div>
    <form className="setup-layout" onSubmit={start}>
      <div className="setup-main">
        <section className="surface-panel"><h2><span className="step-number">01</span>Choose your focus</h2>
          <div className="mode-options">{modeChoices.map(({ mode: choice, icon: Icon, desc }) => <button type="button" key={choice} disabled={busy} aria-pressed={mode === choice} className={`mode-option ${mode === choice ? 'selected' : ''}`} onClick={() => { setParams({ mode: choice }, { replace: true }); setError(''); }}><Icon size={23} /><strong>{modeLabels[choice]}</strong><small>{desc}</small></button>)}</div>
        </section>
        <section className="surface-panel">
          <h2><span className="step-number">02</span>{interview ? 'Make it relevant' : pronunciation ? 'Choose what to work on' : 'Make it a real conversation'}</h2>
          <p className="setup-guidance">{interview ? 'Build a session around the role you want.' : pronunciation ? 'Work on one aspect of clear speech, with words you can use every day.' : 'Choose a situation and a speaking skill you want to build.'}</p>
          <div className="form-grid">
            {interview ? <>
              <label>Target job role<select value={role} onChange={e => { setRole(e.target.value); update('jobRole', e.target.value === 'Other' ? '' : e.target.value); }}>{roles.map(r => <option key={r}>{r}</option>)}</select></label>
              <label>Experience level<select value={setup.level} onChange={e => update('level', e.target.value as Setup['level'])}><option value="BEGINNER">Entry level / beginner</option><option value="INTERMEDIATE">Mid level / intermediate</option><option value="ADVANCED">Senior / advanced</option></select></label>
              {role === 'Other' && <label className="full-width">Your role<input required maxLength={120} value={setup.jobRole ?? ''} onChange={e => update('jobRole', e.target.value)} placeholder="e.g. Mechanical Engineer" /></label>}
            </> : <>
              {pronunciation ? <>
                <label>Pronunciation focus<select value={practice?.pronunciationFocus ?? 'WORD_STRESS'} onChange={e => updatePractice('pronunciationFocus', e.target.value as PracticeOptions['pronunciationFocus'])}>{options(pronunciationFocuses)}</select></label>
                <label>Exercise format<select value={practice?.exerciseFormat ?? 'SENTENCES'} onChange={e => updatePractice('exerciseFormat', e.target.value as PracticeOptions['exerciseFormat'])}>{Object.entries(exerciseFormats).map(([value, label]) => <option key={value} value={value} disabled={value === 'WORDS' && practice?.pronunciationFocus !== 'SOUNDS' && practice?.pronunciationFocus !== 'WORD_STRESS'}>{label}</option>)}</select></label>
              </> : <>
                <label>Conversation situation<select value={practice?.scenario ?? 'EVERYDAY'} onChange={e => updatePractice('scenario', e.target.value as PracticeOptions['scenario'])}>{options(scenarios)}</select></label>
                <label>Speaking goal<select value={practice?.communicationGoal ?? 'CLARITY'} onChange={e => updatePractice('communicationGoal', e.target.value as PracticeOptions['communicationGoal'])}>{options(communicationGoals)}</select></label>
              </>}
              <label className="full-width">English comfort level<select value={setup.level} onChange={e => update('level', e.target.value as Setup['level'])}>{options(englishLevels)}</select><small className="field-help">Choose what feels comfortable today. This is not a language test.</small></label>
              <label className="full-width">{pronunciation ? 'Specific sounds or words' : 'Topic to talk about'} <span className="optional-label">Optional</span><input maxLength={200} value={practice?.topic ?? ''} onChange={e => updatePractice('topic', e.target.value)} placeholder={pronunciation ? 'e.g. th sounds, r and l, or a difficult word' : 'e.g. travel, a college project, or a recent experience'} /></label>
              {customText && <label className="full-width">Your read-aloud text<textarea required maxLength={1200} rows={4} value={practice?.customText ?? ''} onChange={e => updatePractice('customText', e.target.value)} placeholder="Paste the words or passage you want to practise…" /><small className="field-help">{practice?.customText?.length ?? 0}/1200 characters · Longer text is split into short exercises.</small></label>}
            </>}
          </div>
          {interview ? <>
            <label className="document-select">Company context <span>Optional</span><select value={setup.documentId || ''} onChange={e => update('documentId', e.target.value || undefined)}><option value="">General practice — no document</option>{documents.map(d => <option value={d.id} key={d.id}>{d.filename}</option>)}</select></label>
            {docError && <p className="inline-warning">{docError}</p>}
            <Link to="/documents" className="text-link"><FileText size={15} />Upload a job description or company document</Link>
          </> : <p className="practice-note"><AudioLines size={16} />{pronunciation ? 'Record your voice in the room for speech feedback. Typed answers cannot show pronunciation.' : 'Use your voice for delivery feedback, or type to practise wording and conversation.'}</p>}
        </section>
        <section className="surface-panel"><h2><span className="step-number">03</span>{interview ? 'Set the atmosphere' : 'Set your pace'}</h2>
          <div className="form-grid"><label>Coaching style<select value={setup.style} onChange={e => update('style', e.target.value as Setup['style'])}>{options(styleLabels[mode])}</select></label><label>Number of {units}<select value={setup.questionCount} onChange={e => update('questionCount', Number(e.target.value))}><option value={3}>3 — a quick warm-up</option><option value={5}>5 — focused practice</option><option value={10}>10 — a deeper session</option><option value={15}>15 — extended practice</option></select></label></div>
        </section>
      </div>
      <aside className="setup-summary" aria-label="Your practice plan">
        <span className="eyebrow">YOUR PRACTICE PLAN</span><div className="summary-orb"><AudioLines size={45} /></div><h2>{modeLabels[mode]}</h2>
        <p>{interview ? `A one-on-one session, tailored to ${setup.jobRole || 'your role'}.` : pronunciation ? 'Build clearer speech, one focused exercise at a time.' : 'Find your words in a conversation that fits your day.'}</p>
        <dl>{details.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}<div><dt>{interview ? 'Questions' : pronunciation ? 'Exercises' : 'Turns'}</dt><dd>{setup.questionCount}</dd></div><div><dt>{interview ? 'Experience' : 'English level'}</dt><dd>{interview ? setup.level.toLowerCase() : englishLevels[setup.level]}</dd></div><div><dt>Coaching</dt><dd>{styleLabels[mode][setup.style]}</dd></div>{interview && <div><dt>Company context</dt><dd>{setup.documentId ? 'Included' : 'General practice'}</dd></div>}<div><dt>{pronunciation ? 'For speech feedback' : 'Answer your way'}</dt><dd>{pronunciation ? 'Voice recording' : 'Voice or text'}</dd></div></dl>
        <button className="primary-button" disabled={busy || !valid}>{busy ? <><LoaderCircle className="spin" size={18} />Preparing your room…</> : <>Start my session <ArrowRight size={18} /></>}</button>
        <small>Your microphone is only accessed when you press Record. Recordings are sent to Gemini for transcription and coaching.</small>{error && <div className="error-box" role="alert">{error}</div>}
      </aside>
    </form>
  </>;
}
