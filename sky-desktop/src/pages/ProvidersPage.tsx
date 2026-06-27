import { useState, useEffect } from 'react';
import { api, ProviderStatus } from '../api/client';

export default function ProvidersPage() {
  const [providers, setProviders] = useState<ProviderStatus[]>([]);
  const [metrics, setMetrics] = useState<Record<string, unknown>>({});
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      api.providerStatus().then(setProviders),
      api.agentMetrics().then(setMetrics),
    ]).finally(() => setLoading(false));
  }, []);

  const agents = ['ceo', 'memory', 'tool', 'research', 'consolidation'] as const;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="page-header">
        <h2>Providers & Agents</h2>
        <button className="secondary" onClick={() => {
          setLoading(true);
          Promise.all([api.providerStatus().then(setProviders), api.agentMetrics().then(setMetrics)])
            .finally(() => setLoading(false));
        }}>Refresh</button>
      </div>
      <div className="page-content">
        <h3 style={{ marginBottom: 12, fontSize: 14, color: 'var(--text-muted)' }}>LLM Providers</h3>
        {loading ? <div className="empty-state"><span className="spinner" /></div> : (
          <table>
            <thead>
              <tr>
                <th>Provider</th>
                <th>Status</th>
                <th>Free</th>
                <th>Last Error</th>
              </tr>
            </thead>
            <tbody>
              {providers.map(p => (
                <tr key={p.providerId}>
                  <td><strong>{p.providerId}</strong></td>
                  <td>
                    <span className={`badge ${p.available ? 'badge-green' : 'badge-red'}`}>
                      {p.available ? 'Available' : 'Unavailable'}
                    </span>
                  </td>
                  <td>{p.free ? <span className="badge badge-green">Free</span> : <span className="badge badge-yellow">Paid</span>}</td>
                  <td style={{ fontSize: 12, color: 'var(--text-muted)' }}>{p.lastError || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <h3 style={{ margin: '24px 0 12px', fontSize: 14, color: 'var(--text-muted)' }}>Agent Metrics</h3>
        <div className="stat-grid">
          {agents.map(name => {
            const m = metrics[name] as Record<string, unknown> | undefined;
            if (!m) return null;
            return (
              <div className="stat-card" key={name}>
                <div className="stat-label">{name}</div>
                <div style={{ fontSize: 12, marginTop: 4 }}>
                  <span className={`badge ${m.health === 'HEALTHY' ? 'badge-green' : m.health === 'DEGRADED' ? 'badge-yellow' : 'badge-red'}`}>
                    {m.health as string}
                  </span>
                </div>
                <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 6 }}>
                  {m.successful as number}/{m.totalRequests as number} ok | {(m.averageLatencyMs as number).toFixed(0)}ms avg
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
