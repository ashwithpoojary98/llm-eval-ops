"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode,
} from "react";
import { useRouter } from "next/navigation";
import { api } from "./api";
import { User, LoginRequest } from "./types";

interface AuthContextType {
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (credentials: LoginRequest) => Promise<boolean>;
  logout: () => Promise<void>;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  // Handle auth failure - called by API client when token refresh fails
  const handleAuthFailure = useCallback(() => {
    setUser(null);
    router.push("/login?expired=true");
  }, [router]);

  // Set up auth failure callback on API client
  useEffect(() => {
    api.setAuthFailureCallback(handleAuthFailure);
    return () => {
      api.setAuthFailureCallback(null);
    };
  }, [handleAuthFailure]);

  // Load user from localStorage on mount
  useEffect(() => {
    const storedUser = localStorage.getItem("user");
    const accessToken = localStorage.getItem("accessToken");

    if (storedUser && accessToken) {
      try {
        setUser(JSON.parse(storedUser));
      } catch {
        localStorage.removeItem("user");
      }
    }
    setLoading(false);
  }, []);

  const login = useCallback(async (credentials: LoginRequest): Promise<boolean> => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.login(credentials);

      if (response.success && response.data) {
        const userData: User = {
          userId: response.data.userId,
          email: response.data.email,
          fullName: response.data.fullName,
          role: response.data.role,
          organizationId: response.data.organizationId,
          organizationName: response.data.organizationName,
          mustChangePassword: response.data.mustChangePassword,
        };
        setUser(userData);
        return true;
      } else {
        setError(response.error || response.message || "Login failed");
        return false;
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    setLoading(true);
    try {
      await api.logout();
    } finally {
      setUser(null);
      setLoading(false);
      router.push("/login");
    }
  }, [router]);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{ user, loading, error, login, logout, clearError }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
