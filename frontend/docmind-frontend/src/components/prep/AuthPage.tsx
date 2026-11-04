import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, AudioLines, Check, Eye, EyeOff, LoaderCircle } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { errorMessage } from '../../services/interviewApi';
import Preferences from './Preferences';
import './prep.css';
import './premium.css';

export default function AuthPage({ register = false }: { register?: boolean }) {
  const auth = useAuthStore(); const navigate = useNavigate();
  const [username, setUsername] = useState(''); const [email, setEmail] = useState(''); const [password, setPassword] = useState('');
  const [visible, setVisible] = useState(false); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  useEffect(() => { if (auth.token) navigate('/', { replace: true }); }, [auth.token, navigate]);
  async function submit(e: React.FormEvent) {
    e.preventDefault(); if (busy) return; setBusy(true); setError('');
    try { if (register) await auth.register(username.trim(), email.trim(), password); await auth.login(username.trim(), password); navigate('/'); }
    catch (e) { setError(errorMessage(e)); } finally { setBusy(false); }
  }
  return <div className="prep-app prep-auth"><section className="auth-story"><Link to="/" className="prep-brand"><span><AudioLines size={23} /></span>prep<span className="brand-light">room</span><sup>AI</sup></Link><div className="auth-story-content"><span className="eyebrow">YOUR NEXT CHAPTER STARTS HERE</span><h1>Find your words.<br />Build your confidence.<br /><em>Be ready.</em></h1><p>A little practice today can change how you show up tomorrow. Meet your personal AI interview and communication coach.</p><div className="auth-benefits"><span><Check size={16} />Interviews tailored to your role</span><span><Check size={16} />Real conversations, helpful feedback</span><span><Check size={16} />Your voice. Your pace. Your progress.</span></div></div><small>Powered by Gemini · Built on DocMind</small></section><section className="auth-form-side"><div className="auth-preferences"><Preferences /></div><div className="auth-form"><span className="eyebrow">{register ? 'MAKE ROOM FOR GROWTH' : 'GOOD TO HAVE YOU BACK'}</span><h2>{register ? 'Your next step starts here.' : 'Welcome to your practice space.'}</h2><p>{register ? 'Create an account and make your first session count.' : 'Sign in to pick up where you left off.'}</p><form onSubmit={submit}><label htmlFor="username">Username<input id="username" autoComplete="username" required minLength={register ? 3 : undefined} maxLength={50} value={username} onChange={e => setUsername(e.target.value)} placeholder="Your username" /></label>{register && <label htmlFor="email">Email address<input id="email" type="email" autoComplete="email" required maxLength={254} value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com" /></label>}<label htmlFor="password">Password<div className="auth-password"><input id="password" type={visible ? 'text' : 'password'} autoComplete={register ? 'new-password' : 'current-password'} required minLength={register ? 8 : undefined} maxLength={72} value={password} onChange={e => setPassword(e.target.value)} placeholder={register ? 'At least 8 characters' : 'Your password'} /><button type="button" onClick={() => setVisible(v => !v)} aria-label={visible ? 'Hide password' : 'Show password'}>{visible ? <EyeOff size={17} /> : <Eye size={17} />}</button></div></label>{error && <div className="error-box" role="alert">{error}</div>}<button className="primary-button" disabled={busy}>{busy ? <><LoaderCircle className="spin" size={17} />{register ? 'Creating your account…' : 'Signing in…'}</> : <>{register ? 'Create my account' : 'Sign in'}<ArrowRight size={17} /></>}</button></form><div className="auth-switch">{register ? 'Already have an account?' : 'New to PrepRoom?'} <Link to={register ? '/login' : '/register'}>{register ? 'Sign in' : 'Create an account'}</Link></div><div className="auth-footnote"><AudioLines size={17} />Made for progress, one conversation at a time.</div></div></section></div>;
}
