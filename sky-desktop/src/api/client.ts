const BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const token = () => localStorage.getItem('token');

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const t = token();
  if (t) headers['Authorization'] = `Bearer ${t}`;
  const res = await fetch(`${BASE}${path}`, { ...options, headers: { ...headers, ...options?.headers } });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`${res.status} ${res.statusText}: ${text}`);
  }
  return res.json();
}

export interface ChatRequest { message: string; }
export interface ChatResponse {
  response: string; provider: string | null; streaming: boolean;
  intent: string | null; taskType: string | null; routedAgent: string | null;
  durationMs: number; promptTokens: number; completionTokens: number;
}
export interface ProviderStatus {
  providerId: string; available: boolean; free: boolean; lastError: string | null;
}
export interface MemoryEntry {
  id: string; content: string; type: string; source: string;
  confidence: number; createdAt: string; metadata: Record<string, string>;
}
export interface SearchHit { id: string; content: string; source: string; confidence: number; similarity: number; }
export interface TraceEntry {
  id: string; eventType: string; agentId?: string; agentName?: string;
  content: string; timestamp: string; durationMs?: number; success?: boolean;
}
export interface LoopStatus {
  name: string; paused: boolean; healthy: boolean; consecutiveFailures: number;
  lastRun?: string; lastSuccess?: string; lastError?: string;
  priority?: string; budget?: string;
}
export interface AgentMetric {
  name: string; status: string; health: string; totalRequests: number;
  successful: number; failed: number; averageLatencyMs: number; successRate: number;
}
export interface ModeResponse { mode: string; idleTime?: string; }

export interface LoginResult { token: string; username: string; userType: string; }
export interface BootstrapStatus {
  envFileExists: boolean; dockerDetected: boolean; javaDetected: boolean;
  gitDetected: boolean; ollamaDetected: boolean; postgresDetected: boolean;
  rabbitMqDetected: boolean; redisDetected: boolean; workspaceExists: boolean;
  vaultExists: boolean; isComplete: boolean;
  maintainer: boolean;
}
export interface WorkspaceResult { workspacePath: string; workspaceType: string; }
export interface VaultScanResult { markdownFiles: number; topFiles: string[]; }
export interface ExportResult { path: string; sizeBytes: number; }
export interface VaultScanResult { markdownFiles: number; totalFiles?: number; totalSizeBytes?: number; success?: boolean; }
export interface UpdateResult { available: boolean; tagName?: string; name?: string; publishedAt?: string; prerelease?: boolean; }

export const api = {
  chat: (msg: string) => request<ChatResponse>('/api/chat', { method: 'POST', body: JSON.stringify({ message: msg }) }),
  providerStatus: () => request<ProviderStatus[]>('/api/providers/status'),
  agentMetrics: () => request<Record<string, AgentMetric | number>>('/api/agents/metrics'),
  ceoMetrics: () => request<Record<string, unknown>>('/api/agents/ceo/metrics'),
  memorySearch: (q: string, type?: string) => request<SearchHit[]>(`/api/memory/search?q=${encodeURIComponent(q)}${type ? `&type=${type}` : ''}`),
  memoryList: (type?: string, page = 0) => request<{ content: MemoryEntry[] }>(`/api/memory?${type ? `type=${type}&` : ''}page=${page}&size=20`),
  memoryCount: () => request<{ active: number }>('/api/memory/count'),
  createMemory: (content: string, type: string, source: string) => request<MemoryEntry>('/api/memory', {
    method: 'POST', body: JSON.stringify({ content, type, source }),
  }),
  deleteMemory: (id: string) => fetch(`${BASE}/api/memory/${id}`, { method: 'DELETE' }),
  traceTimeline: (page = 0) => request<{ content: TraceEntry[] }>(`/api/trace/timeline?page=${page}&size=30`),
  traceCounts: () => request<Record<string, number>>('/api/trace/counts'),
  traceByAgent: (agentId: string) => request<{ content: TraceEntry[] }>(`/api/trace/agent/${agentId}`),
  loopStatus: () => request<LoopStatus[]>('/api/loops/status'),
  loopMode: () => request<ModeResponse>('/api/loops/mode'),
  setLoopMode: (mode: string) => request<ModeResponse>('/api/loops/mode', { method: 'POST', body: JSON.stringify({ mode }) }),
  pauseLoop: (name: string) => request<Record<string, string>>(`/api/loops/${name}/pause`, { method: 'POST' }),
  resumeLoop: (name: string) => request<Record<string, string>>(`/api/loops/${name}/resume`, { method: 'POST' }),

  githubUrl: () => request<{ url: string }>('/api/auth/github/url'),
  githubLogin: (code: string, redirectUri: string) =>
    request<LoginResult>('/api/auth/github', { method: 'POST', body: JSON.stringify({ code, redirectUri }) }),
  me: () => request<{ username: string; userType: string }>('/api/auth/me'),

  bootstrapStatus: () => request<BootstrapStatus>('/api/bootstrap/status'),
  createWorkspace: () => request<WorkspaceResult>('/api/bootstrap/workspace', { method: 'POST' }),
  scanVault: () => request<VaultScanResult>('/api/bootstrap/vault/scan', { method: 'POST' }),
  acceptTerms: () => request<Record<string, string>>('/api/bootstrap/accept-terms', { method: 'POST' }),
  acceptLgpd: () => request<Record<string, string>>('/api/bootstrap/accept-lgpd', { method: 'POST' }),
  legalStatus: () => request<{ termsAccepted: boolean; lgpdAccepted: boolean }>('/api/bootstrap/legal-status'),

  exportData: () => request<ExportResult>('/api/privacy/export', { method: 'POST' }),
  deleteAccount: () => request<Record<string, string>>('/api/privacy/account', { method: 'DELETE' }),
  clearCache: () => request<Record<string, string>>('/api/privacy/cache', { method: 'POST' }),

  checkUpdate: () => request<UpdateResult>('/api/update/check'),
};

export default api;
