"use client";

import { useState, useEffect, useCallback } from "react";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, EmptyState, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import {
  Project,
  HealthDashboardResponse,
  EndpointHealthSummary,
  HealthStatus,
} from "@/lib/types";
import {
  Activity,
  RefreshCw,
  ChevronDown,
  AlertTriangle,
  CheckCircle,
  XCircle,
  HelpCircle,
} from "lucide-react";
import { toast } from "sonner";

function formatRelativeTime(dateString?: string): string {
  if (!dateString) return "Never";
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSecs = Math.floor(diffMs / 1000);
  if (diffSecs < 60) return `${diffSecs}s ago`;
  const diffMins = Math.floor(diffSecs / 60);
  if (diffMins < 60) return `${diffMins} min ago`;
  const diffHours = Math.floor(diffMins / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays}d ago`;
}

function LatencyCell({ latencyMs }: { latencyMs?: number }) {
  if (latencyMs == null) {
    return <span className="text-gray-400">—</span>;
  }
  const color =
    latencyMs < 500
      ? "text-green-600"
      : latencyMs < 3000
      ? "text-yellow-600"
      : "text-red-600";
  return <span className={`font-medium ${color}`}>{latencyMs}ms</span>;
}

function StatusBadge({ status }: { status: HealthStatus }) {
  switch (status) {
    case "UP":
      return <Badge variant="success">UP</Badge>;
    case "DOWN":
      return <Badge variant="error">DOWN</Badge>;
    case "DEGRADED":
      return <Badge variant="warning">DEGRADED</Badge>;
    default:
      return <Badge variant="default">UNKNOWN</Badge>;
  }
}

function UptimeBar({ pct }: { pct?: number }) {
  const value = pct ?? 0;
  const color =
    value >= 99
      ? "bg-green-500"
      : value >= 90
      ? "bg-yellow-500"
      : "bg-red-500";
  return (
    <div className="flex items-center gap-2">
      <div className="flex-1 h-1.5 bg-gray-200 rounded-full overflow-hidden w-16">
        <div
          className={`h-full ${color} rounded-full`}
          style={{ width: `${Math.min(100, value)}%` }}
        />
      </div>
      <span className="text-xs text-gray-600 tabular-nums">
        {value.toFixed(1)}%
      </span>
    </div>
  );
}

export default function AdminHealthPage() {
  const { user } = useAuth();
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");
  const [dashboard, setDashboard] = useState<HealthDashboardResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [projectsLoading, setProjectsLoading] = useState(true);
  const [lastRefreshed, setLastRefreshed] = useState<Date | null>(null);
  const [checkingEndpoint, setCheckingEndpoint] = useState<string | null>(null);
  const [endpointStatuses, setEndpointStatuses] = useState<
    Record<string, EndpointHealthSummary>
  >({});

  const fetchProjects = useCallback(async () => {
    setProjectsLoading(true);
    try {
      const response = await api.get<Project[]>("/projects");
      if (response.success && response.data) {
        setProjects(response.data);
      } else {
        toast.error(response.error || "Failed to fetch projects");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setProjectsLoading(false);
    }
  }, []);

  const fetchHealthDashboard = useCallback(
    async (projectId: string) => {
      if (!projectId) return;
      setLoading(true);
      try {
        const response = await api.get<HealthDashboardResponse>(
          `/projects/${projectId}/health/dashboard`
        );
        if (response.success && response.data) {
          setDashboard(response.data);
          // Seed per-row statuses from the fetched dashboard
          const map: Record<string, EndpointHealthSummary> = {};
          for (const ep of response.data.endpoints) {
            map[ep.endpointId] = ep;
          }
          setEndpointStatuses(map);
          setLastRefreshed(new Date());
        } else {
          toast.error(response.error || "Failed to fetch health dashboard");
        }
      } catch (err) {
        toast.error(err instanceof Error ? err.message : "An error occurred");
      } finally {
        setLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    if (user?.role === "ADMIN") {
      fetchProjects();
    }
  }, [fetchProjects, user?.role]);

  useEffect(() => {
    if (selectedProjectId) {
      fetchHealthDashboard(selectedProjectId);
    }
  }, [selectedProjectId, fetchHealthDashboard]);

  if (user?.role !== "ADMIN") {
    return (
      <DashboardLayout>
        <Alert variant="error">
          You do not have permission to access this page.
        </Alert>
      </DashboardLayout>
    );
  }

  const handleCheckNow = async (endpoint: EndpointHealthSummary) => {
    setCheckingEndpoint(endpoint.endpointId);
    try {
      const response = await api.post<EndpointHealthSummary>(
        `/api/llm-endpoints/${endpoint.endpointId}/health/check`,
        {}
      );
      if (response.success && response.data) {
        setEndpointStatuses((prev) => ({
          ...prev,
          [endpoint.endpointId]: response.data!,
        }));
        toast.success(`Health check completed for ${endpoint.endpointName ?? endpoint.endpointId}`);
      } else {
        toast.error(response.error || "Health check failed");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setCheckingEndpoint(null);
    }
  };

  const handleRefresh = () => {
    if (selectedProjectId) {
      fetchHealthDashboard(selectedProjectId);
    }
  };

  const endpoints = dashboard
    ? dashboard.endpoints.map((ep) => endpointStatuses[ep.endpointId] ?? ep)
    : [];

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">LLM Health</h1>
            <p className="mt-1 text-sm text-gray-500">
              Monitor the availability and latency of LLM endpoints
            </p>
          </div>
          <div className="flex items-center gap-3">
            {lastRefreshed && (
              <span className="text-xs text-gray-400">
                Last refreshed {formatRelativeTime(lastRefreshed.toISOString())}
              </span>
            )}
            <Button
              variant="outline"
              onClick={handleRefresh}
              disabled={!selectedProjectId || loading}
            >
              <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
              Refresh
            </Button>
          </div>
        </div>

        {/* Project Selector */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-4">
          <div className="flex items-center gap-4">
            <label className="text-sm font-medium text-gray-700 whitespace-nowrap">
              Select Project
            </label>
            <div className="relative flex-1 max-w-xs">
              <select
                value={selectedProjectId}
                onChange={(e) => setSelectedProjectId(e.target.value)}
                className="w-full appearance-none rounded-lg border border-gray-300 px-3 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500 bg-white"
                disabled={projectsLoading}
              >
                <option value="">
                  {projectsLoading ? "Loading projects..." : "Select a project..."}
                </option>
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
            </div>
          </div>
        </div>

        {/* Summary Cards */}
        {dashboard && (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-5">
            <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-4 text-center">
              <p className="text-2xl font-bold text-gray-900">{dashboard.totalEndpoints}</p>
              <p className="text-xs font-medium text-gray-500 uppercase mt-1">Total</p>
            </div>
            <div className="bg-green-50 rounded-xl border border-green-200 shadow-sm p-4 text-center">
              <div className="flex items-center justify-center gap-1 mb-1">
                <CheckCircle className="h-4 w-4 text-green-600" />
                <p className="text-2xl font-bold text-green-700">{dashboard.upCount}</p>
              </div>
              <p className="text-xs font-medium text-green-600 uppercase">UP</p>
            </div>
            <div className="bg-red-50 rounded-xl border border-red-200 shadow-sm p-4 text-center">
              <div className="flex items-center justify-center gap-1 mb-1">
                <XCircle className="h-4 w-4 text-red-600" />
                <p className="text-2xl font-bold text-red-700">{dashboard.downCount}</p>
              </div>
              <p className="text-xs font-medium text-red-600 uppercase">DOWN</p>
            </div>
            <div className="bg-yellow-50 rounded-xl border border-yellow-200 shadow-sm p-4 text-center">
              <div className="flex items-center justify-center gap-1 mb-1">
                <AlertTriangle className="h-4 w-4 text-yellow-600" />
                <p className="text-2xl font-bold text-yellow-700">{dashboard.degradedCount}</p>
              </div>
              <p className="text-xs font-medium text-yellow-600 uppercase">DEGRADED</p>
            </div>
            <div className="bg-gray-50 rounded-xl border border-gray-200 shadow-sm p-4 text-center">
              <div className="flex items-center justify-center gap-1 mb-1">
                <HelpCircle className="h-4 w-4 text-gray-500" />
                <p className="text-2xl font-bold text-gray-600">{dashboard.unknownCount}</p>
              </div>
              <p className="text-xs font-medium text-gray-500 uppercase">UNKNOWN</p>
            </div>
          </div>
        )}

        {/* Endpoints Table */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm">
          {!selectedProjectId ? (
            <EmptyState
              icon={Activity}
              title="No project selected"
              description="Select a project above to view LLM endpoint health."
            />
          ) : loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : endpoints.length === 0 ? (
            <EmptyState
              icon={Activity}
              title="No endpoints found"
              description="This project has no LLM endpoints configured."
            />
          ) : (
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Endpoint
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Model
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Latency
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Uptime (24h)
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Last Checked
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Failures
                  </th>
                  <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {endpoints.map((ep) => (
                  <tr key={ep.endpointId} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div>
                        <p className="font-medium text-gray-900 text-sm">
                          {ep.endpointName ?? ep.endpointId}
                        </p>
                        {ep.providerType && (
                          <Badge variant="info" className="mt-1">
                            {ep.providerType}
                          </Badge>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {ep.modelName ?? "—"}
                    </td>
                    <td className="px-6 py-4">
                      <StatusBadge status={ep.status} />
                      {ep.lastErrorMessage && ep.status !== "UP" && (
                        <p className="text-xs text-red-600 mt-1 max-w-xs truncate" title={ep.lastErrorMessage}>
                          {ep.lastErrorMessage}
                        </p>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      <LatencyCell latencyMs={ep.latencyMs} />
                    </td>
                    <td className="px-6 py-4">
                      <UptimeBar pct={ep.uptimePercentage} />
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {formatRelativeTime(ep.lastCheckedAt)}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      {(ep.consecutiveFailures ?? 0) > 0 ? (
                        <span className="font-semibold text-red-600">
                          {ep.consecutiveFailures}
                        </span>
                      ) : (
                        <span className="text-gray-400">0</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleCheckNow(ep)}
                        loading={checkingEndpoint === ep.endpointId}
                        disabled={checkingEndpoint !== null && checkingEndpoint !== ep.endpointId}
                      >
                        Check Now
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
