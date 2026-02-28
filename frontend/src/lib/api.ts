import { ApiResponse, LoginRequest, LoginResponse } from "./types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api";

type AuthFailureCallback = () => void;

class ApiClient {
  private baseUrl: string;
  private onAuthFailure: AuthFailureCallback | null = null;
  private isRefreshing = false;
  private refreshPromise: Promise<boolean> | null = null;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  setAuthFailureCallback(callback: AuthFailureCallback | null): void {
    this.onAuthFailure = callback;
  }

  private getAccessToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("accessToken");
  }

  private getRefreshToken(): string | null {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("refreshToken");
  }

  private setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("refreshToken", refreshToken);
  }

  private clearTokens(): void {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    localStorage.removeItem("user");
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseUrl}${endpoint}`;
    const accessToken = this.getAccessToken();

    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...options.headers,
    };

    if (accessToken) {
      (headers as Record<string, string>)["Authorization"] = `Bearer ${accessToken}`;
    }

    const response = await fetch(url, {
      ...options,
      headers,
    });

    // Handle 401 (Unauthorized) - try to refresh token
    // Note: 403 (Forbidden) means authenticated but not authorized - don't refresh
    if (response.status === 401 && this.getRefreshToken()) {
      const refreshed = await this.refreshAccessToken();
      if (refreshed) {
        // Retry the original request with new token
        const newAccessToken = this.getAccessToken();
        (headers as Record<string, string>)["Authorization"] = `Bearer ${newAccessToken}`;
        const retryResponse = await fetch(url, { ...options, headers });

        // If still getting 401 after refresh, session is truly invalid
        if (retryResponse.status === 401) {
          this.handleAuthFailure();
          return this.parseResponse<T>(retryResponse);
        }

        return this.parseResponse<T>(retryResponse);
      } else {
        // Refresh failed, trigger auth failure
        this.handleAuthFailure();
        return {
          success: false,
          statusCode: 401,
          error: "Session expired. Please login again.",
          timestamp: new Date().toISOString(),
        };
      }
    }

    return this.parseResponse<T>(response);
  }

  private handleAuthFailure(): void {
    this.clearTokens();
    if (this.onAuthFailure) {
      this.onAuthFailure();
    }
  }

  private async parseResponse<T>(response: Response): Promise<ApiResponse<T>> {
    // Handle empty responses (204 No Content, etc.)
    const contentLength = response.headers.get("content-length");
    const contentType = response.headers.get("content-type");

    if (response.status === 204 || contentLength === "0" || !contentType?.includes("application/json")) {
      // Empty or non-JSON response
      if (response.ok) {
        return {
          success: true,
          statusCode: response.status,
          timestamp: new Date().toISOString(),
        };
      }
      return {
        success: false,
        statusCode: response.status,
        error: "Request failed",
        timestamp: new Date().toISOString(),
      };
    }

    // Try to parse JSON
    let json;
    try {
      const text = await response.text();
      if (!text || text.trim() === "") {
        // Empty response body
        return {
          success: response.ok,
          statusCode: response.status,
          timestamp: new Date().toISOString(),
        };
      }
      json = JSON.parse(text);
    } catch {
      // Failed to parse JSON
      return {
        success: response.ok,
        statusCode: response.status,
        error: response.ok ? undefined : "Request failed",
        timestamp: new Date().toISOString(),
      };
    }

    // Handle wrapped ApiResponse format
    if ('success' in json && ('data' in json || 'error' in json)) {
      return json as ApiResponse<T>;
    }

    // Handle direct response (data returned without wrapper)
    if (response.ok) {
      return {
        success: true,
        statusCode: response.status,
        data: json as T,
        timestamp: new Date().toISOString(),
      };
    }

    // Handle error response
    return {
      success: false,
      statusCode: response.status,
      error: json.message || json.error || "Request failed",
      timestamp: new Date().toISOString(),
    };
  }

  private async refreshAccessToken(): Promise<boolean> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) return false;

    // Prevent multiple simultaneous refresh attempts
    if (this.isRefreshing && this.refreshPromise) {
      return this.refreshPromise;
    }

    this.isRefreshing = true;
    this.refreshPromise = this.doRefreshToken(refreshToken);

    try {
      return await this.refreshPromise;
    } finally {
      this.isRefreshing = false;
      this.refreshPromise = null;
    }
  }

  private async doRefreshToken(refreshToken: string): Promise<boolean> {
    try {
      const response = await fetch(`${this.baseUrl}/auth/refresh`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ refreshToken }),
      });

      if (!response.ok) return false;

      const json = await response.json();

      // Handle both wrapped ApiResponse and direct response formats
      let accessToken: string | undefined;
      let newRefreshToken: string | undefined;

      if ('success' in json && json.data) {
        // Wrapped response: { success: true, data: { accessToken, refreshToken } }
        accessToken = json.data.accessToken;
        newRefreshToken = json.data.refreshToken;
      } else if ('accessToken' in json) {
        // Direct response: { accessToken, refreshToken }
        accessToken = json.accessToken;
        newRefreshToken = json.refreshToken;
      }

      if (accessToken && newRefreshToken) {
        this.setTokens(accessToken, newRefreshToken);
        return true;
      }

      return false;
    } catch {
      return false;
    }
  }

  async login(credentials: LoginRequest): Promise<ApiResponse<LoginResponse>> {
    const response = await fetch(`${this.baseUrl}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(credentials),
    });

    const json = await response.json();

    // Handle both wrapped ApiResponse and direct response formats
    let result: ApiResponse<LoginResponse>;

    if ('success' in json && 'data' in json) {
      // Backend returns wrapped response
      result = json;
    } else if ('accessToken' in json) {
      // Backend returns data directly (successful login)
      result = {
        success: true,
        statusCode: response.status,
        data: json as LoginResponse,
        timestamp: new Date().toISOString(),
      };
    } else {
      // Error response
      result = {
        success: false,
        statusCode: response.status,
        error: json.message || json.error || "Login failed",
        timestamp: new Date().toISOString(),
      };
    }

    if (result.success && result.data) {
      this.setTokens(result.data.accessToken, result.data.refreshToken);
      localStorage.setItem(
        "user",
        JSON.stringify({
          userId: result.data.userId,
          email: result.data.email,
          fullName: result.data.fullName,
          role: result.data.role,
          organizationId: result.data.organizationId,
          organizationName: result.data.organizationName,
          mustChangePassword: result.data.mustChangePassword,
        })
      );
    }

    return result;
  }

  async logout(): Promise<void> {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      try {
        await this.request("/auth/logout", {
          method: "POST",
          body: JSON.stringify({ refreshToken }),
        });
      } catch {
        // Ignore logout errors
      }
    }
    this.clearTokens();
  }

  async get<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: "GET" });
  }

  async post<T>(endpoint: string, data?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "POST",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async put<T>(endpoint: string, data?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "PUT",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async patch<T>(endpoint: string, data?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: "PATCH",
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: "DELETE" });
  }
}

export const api = new ApiClient(API_BASE_URL);
