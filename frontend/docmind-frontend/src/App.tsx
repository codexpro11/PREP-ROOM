import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import ProtectedRoute from './components/ProtectedRoute';
import { ErrorBoundary } from './components/ErrorBoundary';
import Login from './pages/Login';
import Register from './pages/Register';
import PrepLayout from './components/prep/PrepLayout';
import PracticeDashboard from './components/prep/PracticeDashboard';
import PracticeSetup from './components/prep/PracticeSetup';
import PracticeSession from './components/prep/PracticeSession';
import CompanyLibrary from './components/prep/CompanyLibrary';

function SessionRoute() { const { id } = useParams(); return <PracticeSession key={id} />; }

export default function App() {
  return <ErrorBoundary><BrowserRouter><Routes>
    <Route path="/login" element={<Login />} /><Route path="/register" element={<Register />} />
    <Route element={<ProtectedRoute />}><Route element={<PrepLayout />}>
      <Route index element={<PracticeDashboard />} /><Route path="practice" element={<PracticeSetup />} />
      <Route path="documents" element={<CompanyLibrary />} /><Route path="history" element={<PracticeDashboard history />} />
      <Route path="session/:id" element={<SessionRoute />} /><Route path="*" element={<Navigate to="/" replace />} />
    </Route></Route>
  </Routes><Toaster position="bottom-right" toastOptions={{ style: { background: '#173f35', color: '#fff' } }} /></BrowserRouter></ErrorBoundary>;
}
