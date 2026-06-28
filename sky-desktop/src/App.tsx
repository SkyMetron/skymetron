import { Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Layout from './components/Layout';
import ChatPage from './pages/ChatPage';
import BrainPage from './pages/BrainPage';
import MemoryPage from './pages/MemoryPage';
import LoopsPage from './pages/LoopsPage';
import ProvidersPage from './pages/ProvidersPage';
import ConfigPage from './pages/ConfigPage';
import LoginPage from './pages/LoginPage';
import LegalPage from './pages/LegalPage';
import BootstrapPage from './pages/BootstrapPage';
import PrivacyPage from './pages/PrivacyPage';
import CallbackPage from './pages/CallbackPage';
import { api } from './api/client';

export default function App() {
  const [authState, setAuthState] = useState<'loading' | 'login' | 'legal' | 'bootstrap' | 'ready'>('loading');

  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    const token = localStorage.getItem('token');
    if (!token) { setAuthState('login'); return; }
    try {
      const me = await api.me();
      localStorage.setItem('username', me.username);
      localStorage.setItem('userType', me.userType);
      const legal = await api.legalStatus();
      if (!legal.termsAccepted || !legal.lgpdAccepted) { setAuthState('legal'); return; }
      const status = await api.bootstrapStatus();
      if (!status.isComplete) { setAuthState('bootstrap'); return; }
      setAuthState('ready');
    } catch {
      localStorage.removeItem('token');
      setAuthState('login');
    }
  };

  if (authState === 'loading') {
    return <div className="loading-screen"><div className="spinner" /><p>Iniciando SkyMetron...</p></div>;
  }

  if (authState === 'login') {
    return (
      <Routes>
        <Route path="/callback" element={<CallbackPage onComplete={() => setAuthState('legal')} />} />
        <Route path="*" element={<LoginPage />} />
      </Routes>
    );
  }

  if (authState === 'legal') {
    return <Routes><Route path="*" element={<LegalPage onComplete={() => setAuthState('bootstrap')} />} /></Routes>;
  }

  if (authState === 'bootstrap') {
    return <Routes><Route path="*" element={<BootstrapPage onComplete={() => setAuthState('ready')} />} /></Routes>;
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Navigate to="/chat" replace />} />
        <Route path="/chat" element={<ChatPage />} />
        <Route path="/brain" element={<BrainPage />} />
        <Route path="/memory" element={<MemoryPage />} />
        <Route path="/loops" element={<LoopsPage />} />
        <Route path="/providers" element={<ProvidersPage />} />
        <Route path="/config" element={<ConfigPage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
      </Route>
    </Routes>
  );
}
