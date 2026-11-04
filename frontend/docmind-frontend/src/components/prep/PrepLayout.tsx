import { NavLink, Outlet, Link, useLocation } from 'react-router-dom';
import { ArrowUpRight, AudioLines, BookOpen, ChartNoAxesCombined, LayoutDashboard, LogOut, Mic, Sparkles } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import Preferences from './Preferences';
import './prep.css';
import './premium.css';

export default function PrepLayout() {
  const user = useAuthStore(s => s.user); const logout = useAuthStore(s => s.logout);
  const location = useLocation();
  const title = location.pathname.startsWith('/session') ? 'Your practice room' : location.pathname === '/documents' ? 'Company library' : location.pathname === '/history' ? 'Practice history' : location.pathname === '/practice' ? 'Set up your practice' : 'Your workspace';
  return <div className="prep-app">
    <aside className="prep-sidebar">
      <Link to="/" className="prep-brand"><span><AudioLines size={23} /></span>prep<span className="brand-light">room</span><sup>AI</sup></Link>
      <div className="sidebar-label">WORKSPACE</div>
      <nav aria-label="Main navigation">
        <NavLink to="/" end><LayoutDashboard size={19} />Overview</NavLink>
        <NavLink to="/practice"><Mic size={19} />Practice room</NavLink>
        <NavLink to="/documents"><BookOpen size={19} />Company library</NavLink>
        <NavLink to="/history"><ChartNoAxesCombined size={19} />My sessions</NavLink>
      </nav>
      <div className="sidebar-note"><Sparkles size={20} /><h3>A little practice.<br />A lot more confidence.</h3><p>Your next opportunity starts with showing up today.</p><Link to="/practice">Let’s practice <ArrowUpRight size={16} /></Link></div>
      <div className="sidebar-profile"><span className="avatar">{user?.username.slice(0, 2).toUpperCase()}</span><div><strong>{user?.username}</strong><small>Personal workspace</small></div><button onClick={logout} aria-label="Sign out"><LogOut size={17} /></button></div>
    </aside>
    <div className="prep-main"><header className="prep-topbar"><span>{title}</span><div className="topbar-controls"><span className="private-label"><span className="status-dot" />Your private practice space</span><Preferences /><button className="mobile-signout" onClick={logout} aria-label="Sign out"><LogOut size={17} /></button></div></header><main className="prep-content"><Outlet /></main><footer className="prep-footer">Made for progress, one conversation at a time.<span>Powered by Gemini</span></footer></div>
  </div>;
}
