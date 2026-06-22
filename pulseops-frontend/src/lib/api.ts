import { useAuthStore } from '../store/authStore';

const BASE_URL = 'http://localhost:8080'; // API Gateway port

interface FetchOptions extends RequestInit {
  bodyData?: any;
}

export async function apiFetch<T>(path: string, options: FetchOptions = {}): Promise<T> {
  const { token, activeOrgId } = useAuthStore.getState();

  const headers = new Headers(options.headers || {});
  
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  if (activeOrgId) {
    headers.set('X-Org-Id', String(activeOrgId));
  }

  const fetchOptions: RequestInit = {
    ...options,
    headers,
  };

  if (options.bodyData) {
    fetchOptions.body = JSON.stringify(options.bodyData);
  }

  const response = await fetch(`${BASE_URL}${path}`, fetchOptions);

  if (response.status === 401) {
    // Session expired, clear state
    useAuthStore.getState().clearSession();
    throw new Error('Unauthorized/Session Expired');
  }

  if (!response.ok) {
    let errMsg = `Request failed with status ${response.status}`;
    try {
      const errJson = await response.json();
      errMsg = errJson.message || errMsg;
    } catch (e) {
      // ignore
    }
    throw new Error(errMsg);
  }

  // Parse JSON standard response
  try {
    const resJson = await response.json();
    return resJson.data as T;
  } catch (e) {
    return {} as T;
  }
}
