import { useState } from 'react';
import { api } from '../api/client';

interface LegalPageProps { onComplete: () => void; }

const TERMS_VERSION = '2026-06-28-v1';
const PRIVACY_VERSION = '2026-06-28-v1';
const APP_VERSION = '0.2.1-beta';

export default function LegalPage({ onComplete }: LegalPageProps) {
  const [accepted, setAccepted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const handleAccept = async () => {
    if (!accepted) return;
    setLoading(true);
    setError('');
    try {
      await api.acceptTerms();
      await api.acceptLgpd();
      onComplete();
    } catch (err: any) {
      setError(err.message || 'Failed to save acceptance');
    } finally {
      setLoading(false);
    }
  };

  const handleDecline = () => {
    localStorage.clear();
    window.location.href = '/';
  };

  const handleExport = async () => {
    setMessage('');
    try {
      const result = await api.exportData();
      setMessage(`Dados exportados para: ${result.path}`);
    } catch (err: any) {
      setMessage('Erro ao exportar dados: ' + err.message);
    }
  };

  const handleDeleteLocalData = async () => {
    if (!window.confirm('Excluir todos os dados locais do SkyMetron neste computador?')) return;
    try {
      await api.deleteAccount();
      localStorage.clear();
      setMessage('Dados locais excluídos. Reinicie o app para começar novamente.');
    } catch (err: any) {
      setMessage('Erro ao excluir dados locais: ' + err.message);
    }
  };

  const handleRevokeGitHub = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('userType');
    setMessage('Sessão GitHub local removida. Faça login novamente para continuar.');
  };

  const handleClearCache = async () => {
    try {
      await api.clearCache();
      setMessage('Cache e logs locais limpos.');
    } catch (err: any) {
      setMessage('Erro ao limpar cache/logs: ' + err.message);
    }
  };

  return (
    <div className="legal-page">
      <div className="legal-card">
        <div className="legal-header">
          <h2>Bem-vindo ao SkyMetron</h2>
          <p>Antes de começar, leia e aceite os termos legais vigentes.</p>
          <p style={{ color: 'var(--text-muted)', fontSize: 12 }}>
            App {APP_VERSION} · Termos {TERMS_VERSION} · Privacidade {PRIVACY_VERSION}
          </p>
        </div>

        <div className="legal-documents">
          <div className="legal-item">
            <div className="legal-item-header"><span>Termos de Uso</span></div>
            <div className="legal-content">
              <p>SkyMetron é uma ferramenta local/desktop para organização, automação assistida por IA, gerenciamento de workspace, integração com GitHub e uso de providers configurados pelo usuário. Esta versão beta pode conter erros, instabilidades e comportamentos inesperados. O uso é por conta e risco do usuário.</p>
            </div>
          </div>

          <div className="legal-item">
            <div className="legal-item-header"><span>Política de Privacidade e Aviso LGPD</span></div>
            <div className="legal-content">
              <p>Dados ficam localmente sempre que possível. Dados podem ir a terceiros apenas quando você configura GitHub ou providers externos e usa funcionalidades que dependem deles. Tokens, chaves, vault, prompts e logs devem ser tratados como dados sensíveis pelo usuário.</p>
            </div>
          </div>

          <div className="legal-links" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            <a className="btn-secondary" href="https://github.com/SkyMetron/skymetron/blob/main/TERMS_OF_USE.md" target="_blank" rel="noreferrer">Termos de Uso</a>
            <a className="btn-secondary" href="https://github.com/SkyMetron/skymetron/blob/main/PRIVACY_POLICY.md" target="_blank" rel="noreferrer">Política de Privacidade</a>
            <a className="btn-secondary" href="https://github.com/SkyMetron/skymetron/blob/main/LGPD.md" target="_blank" rel="noreferrer">Aviso LGPD</a>
          </div>

          <div className="legal-actions-grid" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            <button className="btn-secondary" onClick={handleExport}>Exportar meus dados</button>
            <button className="btn-secondary" onClick={handleDeleteLocalData}>Excluir dados locais</button>
            <button className="btn-secondary" onClick={handleRevokeGitHub}>Revogar login GitHub</button>
            <button className="btn-secondary" onClick={() => setMessage('Remova chaves de API pelo arquivo .env/configuração local antes de continuar.')}>Remover chaves de API</button>
            <button className="btn-secondary" onClick={handleClearCache}>Limpar cache/logs</button>
          </div>

          <label className="legal-checkbox">
            <input
              aria-label="Li e aceito os Termos de Uso e a Política de Privacidade."
              type="checkbox"
              checked={accepted}
              onChange={() => setAccepted(!accepted)}
            />
            <span>Li e aceito os Termos de Uso e a Política de Privacidade.</span>
          </label>
        </div>

        {error && <div className="login-error">{error}</div>}
        {message && <div className="privacy-message">{message}</div>}

        <div className="legal-actions">
          <button className="btn-primary" onClick={handleAccept} disabled={!accepted || loading}>
            {loading ? 'Salvando...' : 'Continuar'}
          </button>
          <button className="btn-secondary" onClick={handleDecline}>
            Recusar e Sair
          </button>
        </div>
      </div>
    </div>
  );
}
