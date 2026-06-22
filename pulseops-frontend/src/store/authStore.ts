import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface Organization {
  id: number;
  name: String;
  slug: string;
}

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
}

interface AuthState {
  token: string | null;
  refreshToken: string | null;
  user: User | null;
  organizations: Organization[];
  activeOrgId: number | null;
  isAuthenticated: boolean;
  setSession: (token: string, refreshToken: string, user: User, orgs: Organization[]) => void;
  clearSession: () => void;
  setActiveOrgId: (orgId: number) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      user: null,
      organizations: [],
      activeOrgId: null,
      isAuthenticated: false,
      setSession: (token, refreshToken, user, orgs) => {
        const defaultOrgId = orgs.length > 0 ? orgs[0].id : null;
        set({
          token,
          refreshToken,
          user,
          organizations: orgs,
          activeOrgId: defaultOrgId,
          isAuthenticated: true,
        });
      },
      clearSession: () => {
        set({
          token: null,
          refreshToken: null,
          user: null,
          organizations: [],
          activeOrgId: null,
          isAuthenticated: false,
        });
      },
      setActiveOrgId: (orgId) => {
        set({ activeOrgId: orgId });
      },
    }),
    {
      name: 'pulseops-auth-storage',
    }
  )
);
