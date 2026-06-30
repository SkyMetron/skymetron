import { Link } from 'react-router-dom';

export default function ConfigPage() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="page-header">
        <h2>Settings</h2>
      </div>
      <div className="page-content">
        <div className="card">
          <div className="card-header"><span>LLM Providers</span></div>
          <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
            API keys are configured via the <code>.env</code> file in the sky-monolith directory.
            <br />
            Supported providers: Mistral, NVIDIA, Google Gemini, Groq, OpenRouter, Ollama Local.
          </div>
        </div>

        <div className="card">
          <div className="card-header"><span>API Endpoint</span></div>
          <div style={{ fontSize: 13 }}>
            <div style={{ marginBottom: 8 }}>The desktop connects to the SkyMetron backend at:</div>
            <input value="http://localhost:8080" readOnly />
            <div style={{ marginTop: 8, color: 'var(--text-muted)', fontSize: 12 }}>
              Start the backend with: <code>mvn spring-boot:run -pl sky-core</code>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-header"><span>Privacidade</span></div>
          <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
            Gerencie seus dados, exporte informações ou exclua sua conta.
          </div>
          <Link to="/privacy" className="btn-secondary" style={{ display: 'inline-block', marginTop: 12, textDecoration: 'none' }}>
            Abrir Privacidade
          </Link>
        </div>

        <div className="card">
          <div className="card-header"><span>About SkyMetron</span></div>
          <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
            Version 0.2.1-beta &middot; AI Operating System
            <br />
            10 agents &middot; Multi-provider LLM (100% free) &middot; PostgreSQL + pgvector &middot; RabbitMQ event bus
            <br />
            Electron desktop &middot; Observability with Grafana/Prometheus/Loki
          </div>
        </div>
      </div>
    </div>
  );
}
