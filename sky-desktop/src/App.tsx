import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import ChatPage from './pages/ChatPage';
import BrainPage from './pages/BrainPage';
import MemoryPage from './pages/MemoryPage';
import LoopsPage from './pages/LoopsPage';
import ProvidersPage from './pages/ProvidersPage';
import ConfigPage from './pages/ConfigPage';

export default function App() {
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
      </Route>
    </Routes>
  );
}
