import { useState } from 'react';
import { api } from '../api/client';

interface LegalPageProps { onComplete: () => void; }

export default function LegalPage({ onComplete }: LegalPageProps) {
  const [termsAccepted, setTermsAccepted] = useState(false);
  const [lgpdAccepted, setLgpdAccepted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showTerms, setShowTerms] = useState(false);
  const [showLgpd, setShowLgpd] = useState(false);

  const handleAccept = async () => {
    if (!termsAccepted || !lgpdAccepted) return;
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

  return (
    <div className="legal-page">
      <div className="legal-card">
        <div className="legal-header">
          <h2>Bem-vindo ao SkyMetron</h2>
          <p>Antes de começar, leia e aceite nossos termos.</p>
        </div>

        <div className="legal-documents">
          <div className="legal-item">
            <div className="legal-item-header" onClick={() => setShowTerms(!showTerms)}>
              <span>Termos de Serviço</span>
              <span className="legal-toggle">{showTerms ? '▲' : '▼'}</span>
            </div>
            {showTerms && (
              <div className="legal-content">
                <p>O SkyMetron é fornecido sob licença MIT. O software é fornecido "no estado em que se encontra", sem garantias. O usuário é responsável pelo conteúdo enviado aos provedores LLM e pelo uso das respostas geradas. É proibido utilizar para atividades ilegais, spam ou violação de direitos autorais.</p>
              </div>
            )}
            <label className="legal-checkbox">
              <input type="checkbox" checked={termsAccepted} onChange={() => setTermsAccepted(!termsAccepted)} />
              <span>Aceito os Termos de Serviço</span>
            </label>
          </div>

          <div className="legal-item">
            <div className="legal-item-header" onClick={() => setShowLgpd(!showLgpd)}>
              <span>Política de Privacidade (LGPD)</span>
              <span className="legal-toggle">{showLgpd ? '▲' : '▼'}</span>
            </div>
            {showLgpd && (
              <div className="legal-content">
                <p>O SkyMetron coleta apenas dados mínimos via GitHub OAuth para autenticação. API keys de LLM são armazenadas localmente e nunca enviadas a servidores. Nenhum dado é compartilhado com terceiros exceto provedores LLM configurados pelo usuário. Telemetria desabilitada por padrão.</p>
              </div>
            )}
            <label className="legal-checkbox">
              <input type="checkbox" checked={lgpdAccepted} onChange={() => setLgpdAccepted(!lgpdAccepted)} />
              <span>Aceito a Política de Privacidade</span>
            </label>
          </div>
        </div>

        {error && <div className="login-error">{error}</div>}

        <div className="legal-actions">
          <button className="btn-primary" onClick={handleAccept} disabled={!termsAccepted || !lgpdAccepted || loading}>
            {loading ? 'Salvando...' : 'Aceitar e Continuar'}
          </button>
          <button className="btn-secondary" onClick={handleDecline}>
            Recusar e Sair
          </button>
        </div>
      </div>
    </div>
  );
}
