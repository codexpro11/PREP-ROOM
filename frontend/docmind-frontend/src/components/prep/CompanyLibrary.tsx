import { useCallback, useEffect, useRef, useState } from 'react';
import { FileText, Upload, Trash2, ArrowRight, LoaderCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { documentApi } from '../../services/api';
import { errorMessage } from '../../services/interviewApi';
import type { DocumentMetadataDto } from '../../types';

export default function CompanyLibrary() {
  const [documents, setDocuments] = useState<DocumentMetadataDto[]>([]); const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false); const [error, setError] = useState(''); const [progress, setProgress] = useState(0);
  const [deleting, setDeleting] = useState<string | null>(null); const [confirmation, setConfirmation] = useState<string | null>(null);
  const input = useRef<HTMLInputElement>(null);
  const load = useCallback(async () => { try { setDocuments((await documentApi.getMine()).data); } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); } }, []);
  useEffect(() => {
    let active = true;
    documentApi.getMine().then(r => { if (active) setDocuments(r.data); })
      .catch(e => { if (active) setError(errorMessage(e)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);
  async function upload(file?: File) {
    if (!file || busy) return;
    if (!/\.(pdf|docx|txt|md|csv)$/i.test(file.name)) { setError('Choose a PDF, DOCX, TXT, Markdown, or CSV file.'); return; }
    if (file.size > 25 * 1024 * 1024) { setError('Choose a file smaller than 25 MB.'); return; }
    setBusy(true); setError(''); setProgress(0);
    try { const r = await documentApi.upload(file, setProgress); if (r.data.status !== 'INDEXED') throw new Error('Indexing did not finish'); await load(); }
    catch (e) { setError(errorMessage(e)); } finally { setBusy(false); if (input.current) input.current.value = ''; }
  }
  async function remove(id: string) { setDeleting(id); setError(''); try { await documentApi.delete(id); setDocuments(d => d.filter(x => x.id !== id)); setConfirmation(null); } catch (e) { setError(errorMessage(e)); } finally { setDeleting(null); } }
  return <><div className="page-heading"><div><div className="eyebrow">MAKE IT PERSONAL</div><h1>Bring your next opportunity into focus.</h1><p>Upload a job description, company guide, or product brief. Your coach will use it to tailor your questions.</p></div></div>
    <section className="upload-area" onDragOver={e => e.preventDefault()} onDrop={e => { e.preventDefault(); void upload(e.dataTransfer.files[0]); }}><span className="upload-icon">{busy ? <LoaderCircle className="spin" size={28} /> : <Upload size={28} />}</span><h2>{busy ? progress === 100 ? 'Reading and indexing your document…' : `Uploading… ${progress}%` : 'A more relevant interview starts here.'}</h2><p>{busy ? 'Your document is being prepared for personalized questions.' : 'Drop a document here, or choose one from your computer.'}</p><input ref={input} type="file" hidden accept=".pdf,.docx,.txt,.md,.csv" onChange={e => void upload(e.target.files?.[0])} /><button className="primary-button" disabled={busy} onClick={() => input.current?.click()}><Upload size={17} />Choose document</button><small>PDF, DOCX, TXT, Markdown, CSV · Up to 25 MB · One file at a time</small></section>
    {error && <div className="error-box" role="alert">{error}<button onClick={() => { setError(''); void load(); }}>Refresh library</button></div>}
    <div className="section-heading"><h2>Your company documents</h2><span>{documents.length} documents</span></div>
    {loading ? <div className="empty-state">Loading your library…</div> : documents.length ? <div className="document-grid">{documents.map(d => <article className="document-card" key={d.id}><span className="card-icon green"><FileText /></span><span className={`session-badge ${d.status === 'INDEXED' ? 'done' : ''}`}>{d.status === 'INDEXED' ? 'Ready for practice' : d.status.toLowerCase()}</span><h3>{d.filename}</h3><p>{(d.fileSize / 1024).toFixed(0)} KB · {d.totalChunks ?? 0} passages</p><small>Added {new Date(d.createdAt).toLocaleDateString()}</small><div className="document-actions"><Link to="/practice">Use in a session <ArrowRight size={15} /></Link><button aria-label={`Delete ${d.filename}`} disabled={!!deleting} onClick={() => setConfirmation(d.id)}><Trash2 size={17} /></button></div>{confirmation === d.id && <div className="delete-confirm"><p>Delete this document? Active sessions using it will need to be restarted.</p><button onClick={() => setConfirmation(null)} disabled={!!deleting}>Keep it</button><button className="danger-text" disabled={!!deleting} onClick={() => void remove(d.id)}>{deleting ? 'Deleting…' : 'Delete'}</button></div>}</article>)}</div> : <div className="empty-state"><FileText size={28} /><h3>A fresh start for your library.</h3><p>Add your first document above, or start with general role-based practice.</p></div>}
    <p className="privacy-note">Documents are private to your account. Relevant passages are sent to Gemini when you practice with a selected document.</p>
  </>;
}

