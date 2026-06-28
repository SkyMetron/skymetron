import { useState } from 'react';
import { api } from '../api/client';

export default function PrivacyPage() {
  const [exporting, setExporting] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [clearing, setClearing] = useState(false);
  const [message, setMessage] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);

  const handleExport = async () => {
    setExporting(true);
    setMessage('');
    try {
      const result = await api.exportData();
      setMessage(`Dados exportados para: ${result.path} (${(result.sizeBytes / 1024).toFixed(1)} KB)`);
    } catch (err: any) {
      setMessage('Erro ao exportar: ' + err.message);
    } finally {
      setExporting(false);
    }
  };

  const handleClearCache = async () => {
    setClearing(true);
    setMessage('');
    try {
      await api.clearCache();
      setMessage('Cache limpo com sucesso.');
    } catch (err: any) {
      setMessage('Erro ao limpar cache: ' + err.message);
    } finally {
      setClearing(false);
    }
  };

  const handleDeleteAccount = async () => {
    if (!confirmDelete) {
      setConfirmDelete(true);
      setMessage('Tem certeza? Clique novamente para confirmar a exclusão de todos os dados locais.');
      return;
    }
    setDeleting(true);
    setMessage('');
    try {
      await api.deleteAccount();
      localStorage.clear();
      setMessage('Conta local excluída. Redirecionando...');
      setTimeout(() => window.location.href = '/', 2000);
    } catch (err: any) {
      setMessage('Erro ao excluir conta: ' + err.message);
    } finally {
      setDeleting(false);
      setConfirmDelete(false);
    }
  };

  const handleRevokeGitHub = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('userType');
    window.location.href = '/';
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="page-header">
        <h2>Privacidade</h2>
      </div>
      <div className="page-content">
        <div className="card">
          <div className="card-header"><span>Exportar Dados</span></div>
          <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
            Exporta todas as suas memórias, configurações e vault para um arquivo.
          </div>
          <button className="btn-secondary" onClick={handleExport} disabled={exporting} style={{ marginTop: 12 }}>
            {exporting ? 'Exportando...' : 'Exportar Dados'}
          </button>
        </div>

        <div className="card">
          <div className="card-header"><span>Gerenciar GitHub</span></div>
          <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
            Remove o token de acesso ao GitHub. Você precisará fazer login novamente.
          </div>
          <button className="btn-secondary" onClick={handleRevokeGitHub} style={{ marginTop: 12 }}>
            Revogar GitHub
          </button>
        </div>

        <div className="card">
          <div className="card-header"><span>Limpar Cache</span></div>
          <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
            Remove dados temporários e logs locais. Suas memórias e configurações não são afetadas.
          </div>
          <button className="btn-secondary" onClick={handleClearCache} disabled={clearing} style={{ marginTop: 12 }}>
            {clearing ? 'Limpando...' : 'Limpar Cache'}
          </button>
        </div>

        <div className="card" style={{ borderColor: 'var(--danger-color, #e74c3c)' }}>
          <div className="card-header"><span style={{ color: 'var(--danger-color, #e74c3c)' }}>Zona de Perigo</span></div>
          <div style={{ fontSize: 13, color: 'var(--text-muted)', lineHeight: 1.6 }}>
            Exclui permanentemente todos os dados locais, incluindo memórias, configurações e vault.
            Esta ação não pode ser desfeita.
          </div>
          <button
            className="btn-danger"
            onClick={handleDeleteAccount}
            disabled={deleting}
            style={{ marginTop: 12 }}
          >
            {deleting ? 'Excluindo...' : confirmDelete ? 'Confirmar Exclusão' : 'Excluir Conta Local'}
          </button>
        </div>

        {message && <div className="privacy-message">{message}</div>}
      </div>
    </div>
  );
}
