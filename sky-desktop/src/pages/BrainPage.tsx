import { useState, useEffect } from 'react';
import { api, TraceEntry } from '../api/client';

export default function BrainPage() {
  const [entries, setEntries] = useState<TraceEntry[]>([]);
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('');

  useEffect(() => {
    Promise.all([
      api.traceTimeline(0).then(r => setEntries(r.content)),
      api.traceCounts().then(setCounts),
    ]).finally(() => setLoading(false));
  }, []);

  const filtered = filter
    ? entries.filter(e => e.eventType?.includes(filter) || e.agentName?.includes(filter))
    : entries;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="page-header">
        <h2>Brain View</h2>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <input
            placeholder="Filter by type or agent..."
            value={filter}
            onChange={e => setFilter(e.target.value)}
            style={{ width: 220 }}
          />
          <span className="badge badge-blue">{Object.keys(counts).length} event types</span>
        </div>
      </div>
      <div className="page-content">
        <div className="stat-grid">
          {Object.entries(counts).slice(0, 8).map(([key, val]) => (
            <div className="stat-card" key={key}>
              <div className="stat-label">{key}</div>
              <div className="stat-value">{val}</div>
            </div>
          ))}
        </div>
        {loading ? (
          <div className="empty-state"><span className="spinner" /></div>
        ) : filtered.length === 0 ? (
          <div className="empty-state"><span className="empty-state-icon">\uD83E\uDDE0</span>No traces found</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Event Type</th>
                <th>Agent</th>
                <th>Content</th>
                <th>Duration</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(e => (
                <tr key={e.id}>
                  <td style={{ whiteSpace: 'nowrap', fontSize: 12 }}>
                    {new Date(e.timestamp).toLocaleTimeString()}
                  </td>
                  <td><span className="badge badge-blue">{e.eventType}</span></td>
                  <td>{e.agentName || e.agentId || '-'}</td>
                  <td style={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {e.content}
                  </td>
                  <td>{e.durationMs ? `${e.durationMs}ms` : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
