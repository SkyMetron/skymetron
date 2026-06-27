const BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${text}`);
  }
  return res.json();
}

export interface ChatRequest {
  message: string;
}

export interface ChatResponse {
  response: string;
  provider: string | null;
  streaming: boolean;
  intent: string | null;
  taskType: string | null;
  routedAgent: string | null;
  durationMs: number;
  promptTokens: number;
  completionTokens: number;
}

export interface ProviderStatus {
  providerId: string;
  available: boolean;
  free: boolean;
  lastError: string | null;
}

export interface MemoryEntry {
  id: string;
  content: string;
  type: string;
  source: string;
  confidence: number;
  createdAt: string;
  metadata: Record<string, string>;
}

export interface SearchHit {
  id: string;
  content: string;
  source: string;
  confidence: number;
  similarity: number;
}

export interface TraceEntry {
  id: string;
  eventType: string;
  agentId?: string;
  agentName?: string;
  content: string;
  timestamp: string;
  durationMs?: number;
  success?: boolean;
}

export interface LoopStatus {
  name: string;
  paused: boolean;
  healthy: boolean;
  consecutiveFailures: number;
  lastRun?: string;
  lastSuccess?: string;
  lastError?: string;
  priority?: string;
  budget?: string;
}

export interface AgentMetric {
  name: string;
  status: string;
  health: string;
  totalRequests: number;
  successful: number;
  failed: number;
  averageLatencyMs: number;
  successRate: number;
}

export interface ModeResponse {
  mode: string;
  idleTime?: string;
}

export const api = {
  chat: (msg: string) =>
    request<ChatResponse>('/api/chat', {
      method: 'POST',
      body: JSON.stringify({ message: msg } as ChatRequest),
    }),
  providerStatus: () => request<ProviderStatus[]>('/api/providers/status'),
  agentMetrics: () => request<Record<string, AgentMetric | number>>('/api/agents/metrics'),
  ceoMetrics: () => request<Record<string, unknown>>('/api/agents/ceo/metrics'),
  memorySearch: (q: string, type?: string) =>
    request<SearchHit[]>(`/api/memory/search?q=${encodeURIComponent(q)}${type ? `&type=${type}` : ''}`),
  memoryList: (type?: string, page = 0) =>
    request<{ content: MemoryEntry[] }>(`/api/memory?${type ? `type=${type}&` : ''}page=${page}&size=20`),
  memoryCount: () => request<{ active: number }>('/api/memory/count'),
  createMemory: (content: string, type: string, source: string) =>
    request<MemoryEntry>('/api/memory', {
      method: 'POST',
      body: JSON.stringify({ content, type, source }),
    }),
  deleteMemory: (id: string) =>
    fetch(`${BASE}/api/memory/${id}`, { method: 'DELETE' }),
  traceTimeline: (page = 0) =>
    request<{ content: TraceEntry[] }>(`/api/trace/timeline?page=${page}&size=30`),
  traceCounts: () => request<Record<string, number>>('/api/trace/counts'),
  traceByAgent: (agentId: string) =>
    request<{ content: TraceEntry[] }>(`/api/trace/agent/${agentId}`),
  loopStatus: () => request<LoopStatus[]>('/api/loops/status'),
  loopMode: () => request<ModeResponse>('/api/loops/mode'),
  setLoopMode: (mode: string) =>
    request<ModeResponse>('/api/loops/mode', {
      method: 'POST',
      body: JSON.stringify({ mode }),
    }),
  pauseLoop: (name: string) =>
    request<Record<string, string>>(`/api/loops/${name}/pause`, { method: 'POST' }),
  resumeLoop: (name: string) =>
    request<Record<string, string>>(`/api/loops/${name}/resume`, { method: 'POST' }),
};
