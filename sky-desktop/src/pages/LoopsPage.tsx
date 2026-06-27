import { useState, useEffect } from 'react';
import { api, LoopStatus, ModeResponse } from '../api/client';

export default function LoopsPage() {
  const [loops, setLoops] = useState<LoopStatus[]>([]);
  const [mode, setMode] = useState<ModeResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const fetch = () => {
    setLoading(true);
    Promise.all([
      api.loopStatus().then(setLoops),
      api.loopMode().then(setMode),
    ]).finally(() => setLoading(false));
  };

  useEffect(() => { fetch(); }, []);

  const togglePause = async (name: string, paused: boolean) => {
    if (paused) await api.resumeLoop(name);
    else await api.pauseLoop(name);
    fetch();
  };

  const setModeAction = async (m: string) => {
    await api.setLoopMode(m);
    fetch();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="page-header">
        <h2>Autonomous Loops</h2>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          {mode && <span className={`badge ${mode.mode === 'ACTIVE' ? 'badge-green' : mode.mode === 'SLEEP' ? 'badge-red' : 'badge-yellow'}`}>{mode.mode}</span>}
          <button className="secondary" onClick={fetch}>Refresh</button>
        </div>
      </div>
      <div className="page-content">
        <div className="card" style={{ marginBottom: 16 }}>
          <div className="card-header"><span>Operation Mode</span></div>
          <div style={{ display: 'flex', gap: 8 }}>
            {['ACTIVE', 'IDLE', 'DEEP', 'SLEEP'].map(m => (
              <button
                key={m}
                className={mode?.mode === m ? '' : 'secondary'}
                onClick={() => setModeAction(m)}
                style={{ flex: 1 }}
              >
                {m}
              </button>
            ))}
          </div>
          {mode?.idleTime && <div style={{ marginTop: 8, fontSize: 12, color: 'var(--text-muted)' }}>Idle: {mode.idleTime}</div>}
        </div>

        {loading ? (
          <div className="empty-state"><span className="spinner" /></div>
        ) : loops.length === 0 ? (
          <div className="empty-state"><span className="empty-state-icon">\u26A1</span>No loops registered</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Loop</th>
                <th>Status</th>
                <th>Health</th>
                <th>Failures</th>
                <th>Last Run</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loops.map(l => (
                <tr key={l.name}>
                  <td><strong>{l.name}</strong></td>
                  <td>
                    <span className={`badge ${l.paused ? 'badge-yellow' : 'badge-green'}`}>
                      {l.paused ? 'Paused' : 'Running'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${l.healthy ? 'badge-green' : 'badge-red'}`}>
                      {l.healthy ? 'Healthy' : 'Failing'}
                    </span>
                  </td>
                  <td>{l.consecutiveFailures}</td>
                  <td style={{ fontSize: 12 }}>
                    {l.lastRun ? new Date(l.lastRun).toLocaleString() : '-'}
                  </td>
                  <td>
                    <button
                      className="secondary"
                      style={{ padding: '4px 10px', fontSize: 12 }}
                      onClick={() => togglePause(l.name, l.paused)}
                    >
                      {l.paused ? 'Resume' : 'Pause'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
