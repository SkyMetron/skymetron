import { useState, useEffect } from 'react';
import { api, SearchHit, MemoryEntry } from '../api/client';

export default function MemoryPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchHit[]>([]);
  const [entries, setEntries] = useState<MemoryEntry[]>([]);
  const [count, setCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [tab, setTab] = useState<'browse' | 'search'>('browse');

  useEffect(() => {
    api.memoryList().then(r => setEntries(r.content));
    api.memoryCount().then(r => setCount(r.active));
  }, []);

  const search = async () => {
    if (!query.trim()) return;
    setLoading(true);
    try {
      const hits = await api.memorySearch(query);
      setResults(hits);
      setTab('search');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="page-header">
        <h2>Memory Vault</h2>
        <div className="stat-value" style={{ fontSize: 14 }}>{count} entries</div>
      </div>
      <div className="page-content">
        <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
          <input
            placeholder="Search memory..."
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && search()}
          />
          <button onClick={search} disabled={loading}>Search</button>
          <button className="secondary" onClick={() => { setTab('browse'); api.memoryList().then(r => setEntries(r.content)); }}>
            Browse All
          </button>
        </div>

        {tab === 'search' && results.length > 0 && (
          <div>
            <h3 style={{ marginBottom: 12, fontSize: 14, color: 'var(--text-muted)' }}>
              Search Results ({results.length})
            </h3>
            {results.map(h => (
              <div className="card" key={h.id}>
                <div className="card-header">
                  <span>Score: {(h.similarity * 100).toFixed(1)}%</span>
                  <span className="badge badge-blue">{h.source}</span>
                </div>
                <div style={{ fontSize: 13, lineHeight: 1.5, whiteSpace: 'pre-wrap' }}>{h.content}</div>
              </div>
            ))}
          </div>
        )}

        {tab === 'browse' && (
          <table>
            <thead>
              <tr>
                <th>Type</th>
                <th>Source</th>
                <th>Content</th>
                <th>Confidence</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {entries.map(e => (
                <tr key={e.id}>
                  <td><span className={`badge ${e.type === 'PROJECT_KNOWLEDGE' ? 'badge-blue' : 'badge-gray'}`}>{e.type}</span></td>
                  <td>{e.source}</td>
                  <td style={{ maxWidth: 350, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {e.content}
                  </td>
                  <td>{(e.confidence * 100).toFixed(0)}%</td>
                  <td style={{ fontSize: 12 }}>{new Date(e.createdAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
