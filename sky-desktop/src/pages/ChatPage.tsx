import { useState, useRef, useEffect } from 'react';
import { api, ChatResponse } from '../api/client';

interface Message {
  role: 'user' | 'agent';
  content: string;
  meta?: string;
}

export default function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    { role: 'agent', content: 'Hello, I\'m SkyMetron. How can I help you today?' },
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [connected, setConnected] = useState<boolean | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    api.ceoMetrics().then(() => setConnected(true)).catch(() => setConnected(false));
  }, []);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages]);

  const send = async () => {
    if (!input.trim() || loading) return;
    const userMsg = input.trim();
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: userMsg }]);
    setLoading(true);

    try {
      const res = await api.chat(userMsg);
      const meta = `intent=${res.intent} | task=${res.taskType} | routedTo=${res.routedAgent} | ${res.durationMs}ms`;
      setMessages(prev => [...prev, { role: 'agent', content: res.response, meta }]);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Unknown error';
      setMessages(prev => [...prev, { role: 'agent', content: `Error: ${msg}`, meta: 'connection error' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <div className="page-header">
        <h2>Chat</h2>
        <div className="connection-status">
          <span className={`status-dot ${connected === true ? 'online' : connected === false ? 'offline' : ''}`} />
          {connected === true ? 'Connected' : connected === false ? 'Disconnected' : 'Checking...'}
        </div>
      </div>
      <div className="chat-messages">
        {messages.map((msg, i) => (
          <div key={i} className={`message ${msg.role}`}>
            <div>{msg.content}</div>
            {msg.meta && <div className="message-meta">{msg.meta}</div>}
          </div>
        ))}
        {loading && (
          <div className="message agent">
            <span className="spinner" /> Thinking...
          </div>
        )}
        <div ref={bottomRef} />
      </div>
      <div className="chat-input-area">
        <input
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && !e.shiftKey && send()}
          placeholder="Type a message..."
          disabled={loading}
        />
        <button onClick={send} disabled={loading || !input.trim()}>Send</button>
      </div>
    </div>
  );
}
