/**
 * NUMMIA — cliente Axios com autenticação JWT e renovação automática do token.
 * Copie para o Front (ex.: src/api/api.ts) junto com types.ts.
 *
 * .env do Front (Vite):  VITE_API_URL=http://localhost:8080
 */
import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type {
  Account, AccountRequest, AssistantMessage, AuthResponse, Budget, BudgetRequest, BudgetSuggestion,
  Category, CategoryRequest, CategoryType, ChangePasswordRequest, ChatResponse, Contribution,
  ContributionRequest, Dashboard, FinancialSummary, Goal, GoalRequest, GoalStatus, LoginRequest,
  MonthlyTotal, Notification, Page, PeriodComparison, Recommendation, RegisterRequest, Anomaly,
  Transaction, TransactionFilters, TransactionRequest, UpdateProfileRequest, User, UUID, YearMonth,
} from './types';

// ---------- Armazenamento dos tokens ----------
// Simples para o MVP. Para mais segurança, mantenha o accessToken só em memória.
const ACCESS_KEY = 'nummia.accessToken';
const REFRESH_KEY = 'nummia.refreshToken';

export const tokenStorage = {
  get access() { return localStorage.getItem(ACCESS_KEY); },
  get refresh() { return localStorage.getItem(REFRESH_KEY); },
  save(auth: AuthResponse) {
    localStorage.setItem(ACCESS_KEY, auth.accessToken);
    localStorage.setItem(REFRESH_KEY, auth.refreshToken);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export const http = axios.create({
  baseURL: `${import.meta.env.VITE_API_URL ?? 'http://localhost:8080'}/api`,
  headers: { 'Content-Type': 'application/json' },
});

http.interceptors.request.use((config) => {
  const token = tokenStorage.access;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ---------- Renovação automática em 401 ----------
// O refresh token é de uso único (rotação): várias requisições que falham ao mesmo
// tempo precisam compartilhar UMA chamada de /auth/refresh.
let refreshing: Promise<string> | null = null;

/** Chamado quando a sessão expira de vez (ex.: redirecionar para /login). */
export let onSessionExpired: () => void = () => { window.location.href = '/login'; };
export function setOnSessionExpired(fn: () => void) { onSessionExpired = fn; }

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStorage.refresh;
  if (!refreshToken) throw new Error('Sem refresh token');
  const { data } = await axios.post<AuthResponse>(`${http.defaults.baseURL}/auth/refresh`, { refreshToken });
  tokenStorage.save(data);
  return data.accessToken;
}

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    const isAuthCall = original?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && original && !original._retry && !isAuthCall) {
      original._retry = true;
      try {
        refreshing ??= refreshAccessToken().finally(() => { refreshing = null; });
        const token = await refreshing;
        original.headers.Authorization = `Bearer ${token}`;
        return http(original);
      } catch {
        tokenStorage.clear();
        onSessionExpired();
      }
    }
    return Promise.reject(error);
  },
);

/** Extrai a mensagem amigável de um erro da API (campo "detail" ou erros por campo). */
export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { detail?: string; errors?: Record<string, string> } | undefined;
    if (data?.errors) return Object.values(data.errors).join('\n');
    if (data?.detail) return data.detail;
    if (!error.response) return 'Não foi possível conectar ao servidor.';
  }
  return 'Ocorreu um erro inesperado.';
}

// ---------- Endpoints ----------

export const authApi = {
  async register(body: RegisterRequest) {
    const { data } = await http.post<AuthResponse>('/auth/register', body);
    tokenStorage.save(data);
    return data;
  },
  async login(body: LoginRequest) {
    const { data } = await http.post<AuthResponse>('/auth/login', body);
    tokenStorage.save(data);
    return data;
  },
  async logout() {
    const refreshToken = tokenStorage.refresh;
    tokenStorage.clear();
    if (refreshToken) await http.post('/auth/logout', { refreshToken }).catch(() => undefined);
  },
};

export const userApi = {
  me: () => http.get<User>('/users/me').then((r) => r.data),
  update: (body: UpdateProfileRequest) => http.put<User>('/users/me', body).then((r) => r.data),
  changePassword: (body: ChangePasswordRequest) => http.put<void>('/users/me/password', body),
  deleteAccount: (password: string) => http.delete<void>('/users/me', { data: { password } }),
};

export const accountApi = {
  list: (includeArchived = false) => http.get<Account[]>('/accounts', { params: { includeArchived } }).then((r) => r.data),
  get: (id: UUID) => http.get<Account>(`/accounts/${id}`).then((r) => r.data),
  create: (body: AccountRequest) => http.post<Account>('/accounts', body).then((r) => r.data),
  update: (id: UUID, body: AccountRequest) => http.put<Account>(`/accounts/${id}`, body).then((r) => r.data),
  remove: (id: UUID) => http.delete<void>(`/accounts/${id}`),
};

export const categoryApi = {
  list: (type?: CategoryType) => http.get<Category[]>('/categories', { params: { type } }).then((r) => r.data),
  get: (id: UUID) => http.get<Category>(`/categories/${id}`).then((r) => r.data),
  create: (body: CategoryRequest) => http.post<Category>('/categories', body).then((r) => r.data),
  update: (id: UUID, body: CategoryRequest) => http.put<Category>(`/categories/${id}`, body).then((r) => r.data),
  remove: (id: UUID) => http.delete<void>(`/categories/${id}`),
};

export const transactionApi = {
  list: (filters: TransactionFilters = {}) =>
    http.get<Page<Transaction>>('/transactions', { params: filters }).then((r) => r.data),
  get: (id: UUID) => http.get<Transaction>(`/transactions/${id}`).then((r) => r.data),
  create: (body: TransactionRequest) => http.post<Transaction>('/transactions', body).then((r) => r.data),
  update: (id: UUID, body: TransactionRequest) => http.put<Transaction>(`/transactions/${id}`, body).then((r) => r.data),
  remove: (id: UUID) => http.delete<void>(`/transactions/${id}`),
};

export const budgetApi = {
  list: (month?: YearMonth) => http.get<Budget[]>('/budgets', { params: { month } }).then((r) => r.data),
  get: (id: UUID) => http.get<Budget>(`/budgets/${id}`).then((r) => r.data),
  create: (body: BudgetRequest) => http.post<Budget>('/budgets', body).then((r) => r.data),
  update: (id: UUID, body: BudgetRequest) => http.put<Budget>(`/budgets/${id}`, body).then((r) => r.data),
  remove: (id: UUID) => http.delete<void>(`/budgets/${id}`),
  copy: (from: YearMonth, to: YearMonth) => http.post<Budget[]>('/budgets/copy', { from, to }).then((r) => r.data),
};

export const goalApi = {
  list: (status?: GoalStatus) => http.get<Goal[]>('/goals', { params: { status } }).then((r) => r.data),
  get: (id: UUID) => http.get<Goal>(`/goals/${id}`).then((r) => r.data),
  create: (body: GoalRequest) => http.post<Goal>('/goals', body).then((r) => r.data),
  update: (id: UUID, body: GoalRequest) => http.put<Goal>(`/goals/${id}`, body).then((r) => r.data),
  remove: (id: UUID) => http.delete<void>(`/goals/${id}`),
  contribute: (id: UUID, body: ContributionRequest) =>
    http.post<Goal>(`/goals/${id}/contributions`, body).then((r) => r.data),
  contributions: (id: UUID) => http.get<Contribution[]>(`/goals/${id}/contributions`).then((r) => r.data),
};

export const dashboardApi = {
  get: (month?: YearMonth) => http.get<Dashboard>('/dashboard', { params: { month } }).then((r) => r.data),
};

export const analysisApi = {
  summary: (month?: YearMonth) => http.get<FinancialSummary>('/analysis/summary', { params: { month } }).then((r) => r.data),
  evolution: (until?: YearMonth, months = 6) =>
    http.get<MonthlyTotal[]>('/analysis/evolution', { params: { until, months } }).then((r) => r.data),
  comparison: (month?: YearMonth) => http.get<PeriodComparison>('/analysis/comparison', { params: { month } }).then((r) => r.data),
  anomalies: (month?: YearMonth) => http.get<Anomaly[]>('/analysis/anomalies', { params: { month } }).then((r) => r.data),
  recommendations: (month?: YearMonth) =>
    http.get<Recommendation[]>('/analysis/recommendations', { params: { month } }).then((r) => r.data),
  budgetSuggestions: () => http.get<BudgetSuggestion[]>('/analysis/budget-suggestions').then((r) => r.data),
};

export const assistantApi = {
  chat: (message: string) => http.post<ChatResponse>('/assistant/chat', { message }).then((r) => r.data),
  messages: (limit = 50) => http.get<AssistantMessage[]>('/assistant/messages', { params: { limit } }).then((r) => r.data),
  clear: () => http.delete<void>('/assistant/messages'),
};

export const notificationApi = {
  list: (unreadOnly = false, page = 0, size = 20) =>
    http.get<Page<Notification>>('/notifications', { params: { unreadOnly, page, size } }).then((r) => r.data),
  unreadCount: () => http.get<{ count: number }>('/notifications/unread-count').then((r) => r.data.count),
  markRead: (id: UUID) => http.patch<Notification>(`/notifications/${id}/read`).then((r) => r.data),
  markAllRead: () => http.patch<{ updated: number }>('/notifications/read-all').then((r) => r.data),
  remove: (id: UUID) => http.delete<void>(`/notifications/${id}`),
};
