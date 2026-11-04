import { modeLabels, statusLabels } from './labels';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, AudioLines, BriefcaseBusiness, Check, Clock3, FileText, MessageCircle, Mic, Plus, TrendingUp } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { errorMessage, interviewApi, type Summary } from '../../services/interviewApi';

export function SessionList({ sessions }: { sessions: Summary[] }) {
  return <div className="session-list">{sessions.map(s => <Link className="session-row" key={s.id} to={`/session/${s.id}`}><span className="session-icon"><BriefcaseBusiness size={20} /></span><div className="session-name"><strong>{s.jobRole}</strong><small>{modeLabels[s.mode]} · {new Date(s.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</small></div><span className={`session-badge ${s.status === 'ANALYZED' ? 'done' : ''}`}>{statusLabels[s.status]}</span><span className="session-count">{s.answered}/{s.questionCount} answers</span><ArrowRight size={17} /></Link>)}</div>;
}
export default function PracticeDashboard({ history = false }: { history?: boolean }) {
  const username = useAuthStore(s => s.user?.username);
  const [sessions, setSessions] = useState<Summary[]>([]); const [loading, setLoading] = useState(true); const [error, setError] = useState('');
  const [attempt, setAttempt] = useState(0);
  useEffect(() => { let active = true; interviewApi.list().then(data => { if (active) setSessions(data); }).catch(e => { if (active) setError(errorMessage(e)); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, [attempt]);
  const finished = sessions.filter(s => s.status === 'ANALYZED').length;
  const answers = sessions.reduce((sum, s) => sum + s.answered, 0);
  return <>
    <div className="page-heading"><div><div className="eyebrow">{history ? 'YOUR JOURNEY' : 'BUILD YOUR NEXT CHAPTER'}</div><h1>{history ? 'Every session is a step forward.' : `Ready for your next opportunity, ${username}?`}</h1><p>{history ? 'Pick up where you left off, or revisit what you learned.' : 'A space to find your words, sharpen your answers, and show up with confidence.'}</p></div><Link to="/practice" className="primary-button"><Plus size={17} />New session</Link></div>
    {!history && <>
      <section className="dashboard-hero"><div className="hero-copy"><span className="hero-tag"><span className="status-dot" />YOUR PERSONAL AI INTERVIEW COACH</span><h2>Good answers start<br />with a little <em>practice.</em></h2><p>Turn “I hope I’m ready” into “I’ve got this.”<br />Practice real conversations, with feedback that helps you grow.</p><Link to="/practice" className="hero-button">Step into your practice room <ArrowRight size={18} /></Link><div className="hero-footnote"><Check size={14} />At your pace <span>·</span><Check size={14} />Tailored to your role</div></div><div className="hero-art" aria-hidden="true"><div className="orbit orbit-one" /><div className="orbit orbit-two" /><div className="art-label top"><span className="tiny-circle"><Mic size={14} /></span>Find your voice</div><div className="voice-orb"><AudioLines size={80} strokeWidth={1.3} /></div><div className="art-label bottom"><span className="tiny-circle"><TrendingUp size={14} /></span>Build your confidence</div><div className="art-star">✦</div></div></section>
      <section className="metric-grid" aria-label="Your practice statistics"><div><span className="metric-icon"><Mic size={20} /></span><div><strong>{loading ? '—' : sessions.length}</strong><span>Practice sessions</span></div></div><div><span className="metric-icon"><MessageCircle size={20} /></span><div><strong>{loading ? '—' : answers}</strong><span>Answers practiced</span></div></div><div><span className="metric-icon"><Check size={20} /></span><div><strong>{loading ? '—' : finished}</strong><span>Feedback reports</span></div></div></section>
      <div className="section-heading"><h2>What would you like to work on?</h2><span>Different goals. Your own pace.</span></div>
      <section className="practice-grid"><Link to="/practice?mode=INTERVIEW" className="practice-card"><span className="card-icon green"><BriefcaseBusiness /></span><span className="card-kicker">GET INTERVIEW READY</span><h3>Mock interviews</h3><p>Role-specific questions, thoughtful follow-ups, and a safe space to try again.</p><span className="card-link">Practice an interview <ArrowRight size={16} /></span></Link><Link to="/practice?mode=COMMUNICATION" className="practice-card"><span className="card-icon peach"><MessageCircle /></span><span className="card-kicker">MAKE YOURSELF HEARD</span><h3>Communication skills</h3><p>Work on fluency and clear expression through one-on-one conversations.</p><span className="card-link">Start a conversation <ArrowRight size={16} /></span></Link><Link to="/practice?mode=PRONUNCIATION" className="practice-card"><span className="card-icon lavender"><AudioLines /></span><span className="card-kicker">SPEAK WITH CLARITY</span><h3>Pronunciation studio</h3><p>Read aloud, practice word stress, and get coaching on your recorded speech.</p><span className="card-link">Find your rhythm <ArrowRight size={16} /></span></Link></section>
      <Link to="/documents" className="company-banner"><span className="card-icon cream"><FileText /></span><div><h3>Your dream company. Your tailored interview.</h3><p>Add a job description or company document to make every question more relevant.</p></div><span>Explore company library <ArrowRight size={17} /></span></Link>
    </>}
    <section className="recent-section"><div className="section-heading"><h2>{history ? 'Your sessions' : 'Recent practice'}</h2>{!history && <Link to="/history">View all sessions <ArrowRight size={15} /></Link>}</div>
      {error ? <div className="error-box" role="alert">{error}<button onClick={() => { setLoading(true); setError(''); setAttempt(a => a + 1); }}>Try again</button></div> : loading ? <div className="empty-state">Loading your sessions…</div> : sessions.length ? <SessionList sessions={history ? sessions : sessions.slice(0, 4)} /> : <div className="empty-state"><Clock3 size={28} /><h3>Your first session is the first step.</h3><p>Once you start practicing, your sessions and feedback will live here.</p><Link to="/practice">Start your first session <ArrowRight size={15} /></Link></div>}
    </section>
  </>;
}


