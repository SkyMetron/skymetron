import { useState, useEffect } from 'react';
import { api, BootstrapStatus, VaultScanResult } from '../api/client';

interface StepStatus {
  name: string;
  status: 'pending' | 'running' | 'done' | 'error';
  message?: string;
}

interface BootstrapPageProps { onComplete: () => void; }

export default function BootstrapPage({ onComplete }: BootstrapPageProps) {
  const [steps, setSteps] = useState<StepStatus[]>([
    { name: 'Verificando ambiente', status: 'running' },
    { name: 'Criando workspace', status: 'pending' },
    { name: 'Escaneando Vault', status: 'pending' },
    { name: 'Configuração inicial', status: 'pending' },
  ]);
  const [status, setStatus] = useState<BootstrapStatus | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    runBootstrap();
  }, []);

  const updateStep = (index: number, update: Partial<StepStatus>) => {
    setSteps(prev => prev.map((s, i) => i === index ? { ...s, ...update } : s));
  };

  const runBootstrap = async () => {
    try {
      const s = await api.bootstrapStatus();
      setStatus(s);
      updateStep(0, { status: 'done', message: s.isComplete ? 'Ambiente completo' : 'Ambiente parcial' });

      if (!s.envFileExists) {
        updateStep(0, { message: '.env não encontrado — será necessário configurar manualmente' });
      }

      updateStep(1, { status: 'running' });
      const ws = await api.createWorkspace();
      updateStep(1, { status: 'done', message: `${ws.workspaceType} — ${ws.workspacePath}` });

      updateStep(2, { status: 'running' });
      const vaultResult: VaultScanResult = await api.scanVault();
      updateStep(2, { status: 'done', message: `${vaultResult.markdownFiles} arquivos .md encontrados` });

      updateStep(3, { status: 'running' });

      if (s.maintainer) {
        updateStep(3, { status: 'done', message: 'Modo desenvolvedor ativado' });
      } else {
        updateStep(3, { status: 'done', message: 'Workspace de usuário criado — configure seus providers' });
      }

      setTimeout(() => onComplete(), 1500);
    } catch (err: any) {
      setError(err.message || 'Bootstrap failed');
      updateStep(0, { status: 'error', message: err.message });
    }
  };

  const statusIcon = (step: StepStatus) => {
    switch (step.status) {
      case 'done': return '✅';
      case 'running': return '⏳';
      case 'error': return '❌';
      default: return '⏸️';
    }
  };

  return (
    <div className="bootstrap-page">
      <div className="bootstrap-card">
        <div className="bootstrap-header">
          <h2>Configurando seu SkyMetron</h2>
          <p>Aguarde enquanto preparamos seu ambiente.</p>
        </div>

        <div className="bootstrap-steps">
          {steps.map((step, i) => (
            <div key={i} className={`bootstrap-step ${step.status}`}>
              <span className="step-icon">{statusIcon(step)}</span>
              <div className="step-content">
                <div className="step-name">{step.name}</div>
                {step.message && <div className="step-message">{step.message}</div>}
              </div>
            </div>
          ))}
        </div>

        {status && (
          <div className="bootstrap-tools">
            <h4>Ferramentas detectadas</h4>
            <div className="tools-grid">
              <ToolItem name="Docker" detected={status.dockerDetected} />
              <ToolItem name="Java" detected={status.javaDetected} />
              <ToolItem name="Git" detected={status.gitDetected} />
              <ToolItem name="Ollama" detected={status.ollamaDetected} />
              <ToolItem name="PostgreSQL" detected={status.postgresDetected} />
              <ToolItem name="RabbitMQ" detected={status.rabbitMqDetected} />
              <ToolItem name="Redis" detected={status.redisDetected} />
            </div>
          </div>
        )}

        {error && <div className="login-error">{error}</div>}
      </div>
    </div>
  );
}

function ToolItem({ name, detected }: { name: string; detected: boolean }) {
  return (
    <div className={`tool-item ${detected ? 'detected' : 'missing'}`}>
      <span className="tool-icon">{detected ? '✅' : '❌'}</span>
      <span className="tool-name">{name}</span>
    </div>
  );
}
