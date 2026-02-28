"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, Alert, Modal, Input } from "@/components/ui";
import { api } from "@/lib/api";
import {
  EvaluationRunSummary,
  EvaluationMetric,
  Dataset,
  LlmEndpointSummary,
  PagedResponse,
  EvaluationStatus,
  EvaluationMode,
  StartEvaluationRequest,
  MetricConfigRequest,
} from "@/lib/types";
import {
  ArrowLeft,
  Play,
  Eye,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Loader2,
  Plus,
  ChevronRight,
} from "lucide-react";
import { toast } from "sonner";

export default function EvaluationsPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const [evaluations, setEvaluations] = useState<EvaluationRunSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<EvaluationStatus | "">("");

  // Start Evaluation Modal
  const [isStartModalOpen, setIsStartModalOpen] = useState(false);
  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [metrics, setMetrics] = useState<EvaluationMetric[]>([]);
  const [endpoints, setEndpoints] = useState<LlmEndpointSummary[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  // Form state
  const [formData, setFormData] = useState<{
    name: string;
    datasetId: string;
    evaluationMode: EvaluationMode;
    targetLlmEndpointId: string;
    judgeLlmEndpointId: string;
    selectedMetrics: string[];
  }>({
    name: "",
    datasetId: "",
    evaluationMode: "PRE_GENERATED",
    targetLlmEndpointId: "",
    judgeLlmEndpointId: "",
    selectedMetrics: [],
  });

  const fetchEvaluations = useCallback(async () => {
    setLoading(true);
    try {
      let url = `/projects/${projectId}/evaluations?size=50`;
      if (statusFilter) {
        url += `&status=${statusFilter}`;
      }
      const response = await api.get<PagedResponse<EvaluationRunSummary>>(url);
      if (response.success && response.data) {
        setEvaluations(response.data.content);
      } else {
        toast.error(response.error || "Failed to fetch evaluations");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [projectId, statusFilter]);

  useEffect(() => {
    fetchEvaluations();
  }, [fetchEvaluations]);

  const fetchFormData = async () => {
    try {
      const [datasetsRes, metricsRes, endpointsRes] = await Promise.all([
        api.get<PagedResponse<Dataset>>(`/projects/${projectId}/datasets?size=100`),
        api.get<EvaluationMetric[]>(`/projects/${projectId}/metrics`),
        api.get<PagedResponse<LlmEndpointSummary>>(
          `/projects/${projectId}/llm-endpoints?size=100`
        ),
      ]);

      if (datasetsRes.success && datasetsRes.data) {
        setDatasets(datasetsRes.data.content);
      }
      if (metricsRes.success && metricsRes.data) {
        setMetrics(metricsRes.data);
      }
      if (endpointsRes.success && endpointsRes.data) {
        setEndpoints(endpointsRes.data.content);
      }
    } catch (err) {
      console.error("Failed to load form data", err);
    }
  };

  const openStartModal = () => {
    fetchFormData();
    setIsStartModalOpen(true);
  };

  const handleStartEvaluation = async () => {
    if (!formData.name.trim()) {
      setFormError("Evaluation name is required");
      return;
    }
    if (!formData.datasetId) {
      setFormError("Please select a dataset");
      return;
    }
    if (formData.evaluationMode === "LIVE_GENERATION" && !formData.targetLlmEndpointId) {
      setFormError("Client LLM is required for Live Generation mode");
      return;
    }
    if (formData.selectedMetrics.length === 0) {
      setFormError("Please select at least one metric");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const metricsConfig: MetricConfigRequest[] = formData.selectedMetrics.map(
        (metricId) => ({
          metricId,
          weight: 1.0,
        })
      );

      const request: StartEvaluationRequest = {
        name: formData.name,
        datasetId: formData.datasetId,
        evaluationMode: formData.evaluationMode,
        targetLlmEndpointId: formData.targetLlmEndpointId || undefined,
        judgeLlmEndpointId: formData.judgeLlmEndpointId || undefined,
        metrics: metricsConfig,
        triggerType: "MANUAL",
      };

      const response = await api.post(
        `/projects/${projectId}/evaluations`,
        request
      );
      if (response.success) {
        setIsStartModalOpen(false);
        setFormData({
          name: "",
          datasetId: "",
          evaluationMode: "PRE_GENERATED",
          targetLlmEndpointId: "",
          judgeLlmEndpointId: "",
          selectedMetrics: [],
        });
        toast.success("Evaluation started successfully");
        fetchEvaluations();
      } else {
        setFormError(response.error || "Failed to start evaluation");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusIcon = (status: EvaluationStatus) => {
    switch (status) {
      case "PENDING":
        return <Clock className="h-4 w-4 text-gray-500" />;
      case "RUNNING":
        return <Loader2 className="h-4 w-4 text-blue-500 animate-spin" />;
      case "COMPLETED":
        return <CheckCircle className="h-4 w-4 text-green-500" />;
      case "FAILED":
        return <XCircle className="h-4 w-4 text-red-500" />;
      case "CANCELLED":
        return <AlertCircle className="h-4 w-4 text-yellow-500" />;
      default:
        return null;
    }
  };

  const getStatusBadgeVariant = (status: EvaluationStatus) => {
    switch (status) {
      case "PENDING":
        return "default";
      case "RUNNING":
        return "info";
      case "COMPLETED":
        return "success";
      case "FAILED":
        return "error";
      case "CANCELLED":
        return "warning";
      default:
        return "default";
    }
  };

  const getMetricsByCategory = () => {
    const grouped: Record<string, EvaluationMetric[]> = {};
    metrics.forEach((metric) => {
      if (!grouped[metric.category]) {
        grouped[metric.category] = [];
      }
      grouped[metric.category].push(metric);
    });
    return grouped;
  };

  const formatCategoryName = (category: string) => {
    return category
      .replace(/_/g, " ")
      .toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  };

  const toggleMetric = (metricId: string) => {
    setFormData((prev) => ({
      ...prev,
      selectedMetrics: prev.selectedMetrics.includes(metricId)
        ? prev.selectedMetrics.filter((id) => id !== metricId)
        : [...prev.selectedMetrics, metricId],
    }));
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-start gap-4">
            <Button variant="ghost" size="sm" onClick={() => router.back()}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Evaluations</h1>
              <p className="mt-1 text-sm text-gray-500">
                Run and track LLM evaluations
              </p>
            </div>
          </div>
          <Button onClick={openStartModal}>
            <Play className="h-4 w-4 mr-2" />
            Start Evaluation
          </Button>
        </div>

        {/* Filters */}
        <div className="flex items-center gap-4">
          <select
            value={statusFilter}
            onChange={(e) =>
              setStatusFilter(e.target.value as EvaluationStatus | "")
            }
            className="rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
          >
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="RUNNING">Running</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        {/* Evaluations List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : evaluations.length > 0 ? (
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Evaluation
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Dataset
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Status
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Progress
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Score
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Created
                  </th>
                  <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {evaluations.map((evaluation) => (
                  <tr key={evaluation.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div>
                        <Link
                          href={`/evaluations/${evaluation.id}`}
                          className="font-medium text-gray-900 hover:text-primary-600"
                        >
                          {evaluation.name}
                        </Link>
                        <p className="text-xs text-gray-500">
                          Run #{evaluation.runNumber}
                        </p>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {evaluation.datasetName}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        {getStatusIcon(evaluation.status)}
                        <Badge variant={getStatusBadgeVariant(evaluation.status)}>
                          {evaluation.status}
                        </Badge>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 bg-gray-200 rounded-full h-2 w-24">
                          <div
                            className="bg-primary-600 h-2 rounded-full transition-all"
                            style={{
                              width: `${
                                evaluation.totalTestCases > 0
                                  ? (evaluation.completedTestCases /
                                      evaluation.totalTestCases) *
                                    100
                                  : 0
                              }%`,
                            }}
                          />
                        </div>
                        <span className="text-xs text-gray-500">
                          {evaluation.completedTestCases}/{evaluation.totalTestCases}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {evaluation.overallScore !== undefined ? (
                        <span
                          className={`font-semibold ${
                            evaluation.overallScore >= 0.8
                              ? "text-green-600"
                              : evaluation.overallScore >= 0.6
                              ? "text-yellow-600"
                              : "text-red-600"
                          }`}
                        >
                          {(evaluation.overallScore * 100).toFixed(1)}%
                        </span>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {new Date(evaluation.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Link
                        href={`/evaluations/${evaluation.id}`}
                        className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm text-primary-600 hover:bg-primary-50 transition-colors"
                      >
                        <Eye className="h-4 w-4" />
                        View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="px-6 py-12 text-center">
              <Play className="h-12 w-12 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-1">
                No evaluations yet
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Start your first evaluation to measure LLM performance
              </p>
              <Button onClick={openStartModal}>
                <Play className="h-4 w-4 mr-2" />
                Start Evaluation
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Start Evaluation Modal */}
      <Modal
        isOpen={isStartModalOpen}
        onClose={() => setIsStartModalOpen(false)}
        title="Start New Evaluation"
        size="lg"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}

          {/* Info Box */}
          <div className="p-3 bg-gray-50 border border-gray-200 rounded-lg text-sm text-gray-600">
            <p className="font-medium text-gray-700 mb-1">How Evaluation Works:</p>
            <ul className="list-disc list-inside space-y-1 text-xs">
              <li><strong>Pre-generated:</strong> Test cases already have LLM outputs to evaluate</li>
              <li><strong>Runtime Generation:</strong> Questions are sent to Client LLM, responses are then evaluated by Judge LLM</li>
            </ul>
          </div>

          <Input
            label="Evaluation Name"
            value={formData.name}
            onChange={(e) =>
              setFormData({ ...formData, name: e.target.value })
            }
            placeholder="e.g., Q4 RAG Performance Test"
            required
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Dataset <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.datasetId}
              onChange={(e) =>
                setFormData({ ...formData, datasetId: e.target.value })
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              required
            >
              <option value="">Select a dataset...</option>
              {datasets.map((dataset) => (
                <option key={dataset.id} value={dataset.id}>
                  {dataset.name} ({dataset.testCaseCount} test cases)
                </option>
              ))}
            </select>
          </div>

          {/* Evaluation Mode */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Evaluation Mode <span className="text-red-500">*</span>
            </label>
            <div className="space-y-2">
              <label className={`flex items-start gap-3 p-3 border rounded-lg cursor-pointer transition-colors ${
                formData.evaluationMode === "PRE_GENERATED"
                  ? "border-primary-500 bg-primary-50"
                  : "border-gray-200 hover:bg-gray-50"
              }`}>
                <input
                  type="radio"
                  name="evaluationMode"
                  value="PRE_GENERATED"
                  checked={formData.evaluationMode === "PRE_GENERATED"}
                  onChange={() =>
                    setFormData({ ...formData, evaluationMode: "PRE_GENERATED", targetLlmEndpointId: "" })
                  }
                  className="mt-0.5"
                />
                <div>
                  <span className="font-medium text-gray-900">Pre-generated Output</span>
                  <p className="text-xs text-gray-500 mt-0.5">
                    Use LLM outputs already provided in test cases. No runtime LLM call needed.
                  </p>
                </div>
              </label>
              <label className={`flex items-start gap-3 p-3 border rounded-lg cursor-pointer transition-colors ${
                formData.evaluationMode === "LIVE_GENERATION"
                  ? "border-primary-500 bg-primary-50"
                  : "border-gray-200 hover:bg-gray-50"
              }`}>
                <input
                  type="radio"
                  name="evaluationMode"
                  value="LIVE_GENERATION"
                  checked={formData.evaluationMode === "LIVE_GENERATION"}
                  onChange={() =>
                    setFormData({ ...formData, evaluationMode: "LIVE_GENERATION" })
                  }
                  className="mt-0.5"
                />
                <div>
                  <span className="font-medium text-gray-900">Runtime Generation (Live)</span>
                  <p className="text-xs text-gray-500 mt-0.5">
                    Send questions to Client LLM at runtime, then evaluate the generated responses.
                  </p>
                </div>
              </label>
            </div>
          </div>

          {/* Client LLM - shown for LIVE_GENERATION mode */}
          {formData.evaluationMode === "LIVE_GENERATION" && (
            <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Client LLM (The LLM Being Tested) <span className="text-red-500">*</span>
              </label>
              {endpoints.length === 0 ? (
                <div className="text-sm text-gray-600">
                  <p className="mb-2">No LLM endpoints configured yet.</p>
                  <Link
                    href={`/projects/${projectId}/llm-endpoints`}
                    className="text-primary-600 hover:text-primary-700 font-medium"
                  >
                    + Add LLM Endpoint first
                  </Link>
                </div>
              ) : (
                <>
                  <select
                    value={formData.targetLlmEndpointId}
                    onChange={(e) =>
                      setFormData({ ...formData, targetLlmEndpointId: e.target.value })
                    }
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500 bg-white"
                    required
                  >
                    <option value="">Select the LLM to test...</option>
                    {endpoints.map((endpoint) => (
                      <option key={endpoint.id} value={endpoint.id}>
                        {endpoint.name} ({endpoint.modelName})
                      </option>
                    ))}
                  </select>
                  <p className="mt-1 text-xs text-blue-700">
                    This LLM receives questions from test cases and generates responses to be evaluated.
                  </p>
                </>
              )}
            </div>
          )}

          {/* Judge LLM */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Judge LLM (Evaluator)
            </label>
            <select
              value={formData.judgeLlmEndpointId}
              onChange={(e) =>
                setFormData({ ...formData, judgeLlmEndpointId: e.target.value })
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="">None (skip LLM-as-Judge metrics)</option>
              {endpoints.map((endpoint) => (
                <option key={endpoint.id} value={endpoint.id}>
                  {endpoint.name} ({endpoint.modelName})
                </option>
              ))}
            </select>
            <p className="mt-1 text-xs text-gray-500">
              Evaluates quality of responses. Required for metrics like Faithfulness, Relevance, etc.
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Metrics <span className="text-red-500">*</span>
            </label>
            <div className="max-h-64 overflow-y-auto border border-gray-200 rounded-lg divide-y divide-gray-200">
              {Object.entries(getMetricsByCategory()).map(
                ([category, categoryMetrics]) => (
                  <div key={category} className="p-3">
                    <h4 className="text-xs font-semibold text-gray-500 uppercase mb-2">
                      {formatCategoryName(category)}
                    </h4>
                    <div className="space-y-2">
                      {categoryMetrics.map((metric) => (
                        <label
                          key={metric.id}
                          className="flex items-start gap-3 cursor-pointer"
                        >
                          <input
                            type="checkbox"
                            checked={formData.selectedMetrics.includes(metric.id)}
                            onChange={() => toggleMetric(metric.id)}
                            className="mt-0.5 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                            disabled={
                              metric.requiresJudgeLlm &&
                              !formData.judgeLlmEndpointId
                            }
                          />
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <span className="text-sm font-medium text-gray-900">
                                {metric.displayName}
                              </span>
                              {metric.requiresJudgeLlm && (
                                <Badge variant="info" className="text-xs">
                                  Requires Judge
                                </Badge>
                              )}
                            </div>
                            {metric.description && (
                              <p className="text-xs text-gray-500">
                                {metric.description}
                              </p>
                            )}
                          </div>
                        </label>
                      ))}
                    </div>
                  </div>
                )
              )}
            </div>
            <p className="mt-1 text-xs text-gray-500">
              Selected: {formData.selectedMetrics.length} metric(s)
            </p>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => setIsStartModalOpen(false)}
            >
              Cancel
            </Button>
            <Button onClick={handleStartEvaluation} loading={submitting}>
              <Play className="h-4 w-4 mr-2" />
              Start Evaluation
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
