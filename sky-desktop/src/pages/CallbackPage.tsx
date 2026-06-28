import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';

const GITHUB_REDIRECT_URI = import.meta.env.VITE_GITHUB_REDIRECT_URI || 'http://localhost:3000/callback';

interface CallbackPageProps { onComplete: () => void; }

export default function CallbackPage({ onComplete }: CallbackPageProps) {
  const [searchParams] = useSearchParams();
  const [error, setError] = useState('');

  useEffect(() => {
    const code = searchParams.get('code');
    if (!code) { setError('No authorization code received from GitHub'); return; }
    api.githubLogin(code, GITHUB_REDIRECT_URI)
      .then(result => {
        localStorage.setItem('token', result.token);
        localStorage.setItem('username', result.username);
        localStorage.setItem('userType', result.userType);
        onComplete();
      })
      .catch(err => setError(err.message));
  }, []);

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-body">
          {error ? (
            <>
              <h3>Erro de Autenticação</h3>
              <p className="login-error">{error}</p>
              <button className="btn-secondary" onClick={() => window.location.href = '/'}>
                Tentar Novamente
              </button>
            </>
          ) : (
            <>
              <div className="spinner" />
              <p>Autenticando com GitHub...</p>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
