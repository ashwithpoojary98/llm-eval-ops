"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, Alert, Modal, Input } from "@/components/ui";
import { api } from "@/lib/api";
import {
  Project,
  EvaluationRunSummary,
  EvaluationMetric,
  Dataset,
  LlmEndpointSummary,
  PagedResponse,
  EvaluationStatus,
  StartEvaluationRequest,
  MetricConfigRequest,
} from "@/lib/types";
import {
  Play,
  Eye,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Loader2,
  FolderKanban,
  Plus,
} from "lucide-react";

export default function AllEvaluationsPage() {
  const [evaluations, setEvaluations] = useState<
    (EvaluationRunSummary & { projectId: string; projectName: string })[]
  >([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // New Evaluation Modal
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [metrics, setMetrics] = useState<EvaluationMetric[]>([]);
  const [endpoints, setEndpoints] = useState<LlmEndpointSummary[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [loadingFormData, setLoadingFormData] = useState(false);

  // Form state
  const [formData, setFormData] = useState<{
    projectId: string;
    name: string;
    datasetId: string;
    judgeLlmEndpointId: string;
    selectedMetrics: string[];
  }>({
    projectId: "",
    name: "",
    datasetId: "",
    judgeLlmEndpointId: "",
    selectedMetrics: [],
  });

  const fetchAllData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const projectsRes = await api.get<PagedResponse<Project>>(
        "/projects?size=100"
      );

      if (!projectsRes.success || !projectsRes.data) {
        setError("Failed to fetch projects");
        return;
      }

      setProjects(projectsRes.data.content);

      const allEvaluations: (EvaluationRunSummary & {
        projectId: string;
        projectName: string;
      })[] = [];

      const fetchPromises = projectsRes.data.content.map(async (project) => {
        const evalRes = await api.get<PagedResponse<EvaluationRunSummary>>(
          `/projects/${project.id}/evaluations?size=20`
        );
        if (evalRes.success && evalRes.data) {
          evalRes.data.content.forEach((evaluation) => {
            allEvaluations.push({
              ...evaluation,
              projectId: project.id,
              projectName: project.name,
            });
          });
        }
      });

      await Promise.all(fetchPromises);

      allEvaluations.sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );

      setEvaluations(allEvaluations);
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  // Fetch project-specific data when project is selected
  const fetchProjectData = async (projectId: string) => {
    if (!projectId) {
      setDatasets([]);
      setMetrics([]);
      setEndpoints([]);
      return;
    }

    setLoadingFormData(true);
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
      console.error("Failed to load project data", err);
    } finally {
      setLoadingFormData(false);
    }
  };

  const handleProjectChange = (projectId: string) => {
    setFormData({
      ...formData,
      projectId,
      datasetId: "",
      judgeLlmEndpointId: "",
      selectedMetrics: [],
    });
    fetchProjectData(projectId);
  };

  const handleStartEvaluation = async () => {
    if (!formData.projectId) {
      setFormError("Please select a project");
      return;
    }
    if (!formData.name.trim()) {
      setFormError("Evaluation name is required");
      return;
    }
    if (!formData.datasetId) {
      setFormError("Please select a dataset");
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
        evaluationMode: "PRE_GENERATED",
        judgeLlmEndpointId: formData.judgeLlmEndpointId || undefined,
        metrics: metricsConfig,
        triggerType: "MANUAL",
      };

      const response = await api.post(
        `/projects/${formData.projectId}/evaluations`,
        request
      );
      if (response.success) {
        setIsNewModalOpen(false);
        resetForm();
        fetchAllData();
      } else {
        setFormError(response.error || "Failed to start evaluation");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setFormData({
      projectId: "",
      name: "",
      datasetId: "",
      judgeLlmEndpointId: "",
      selectedMetrics: [],
    });
    setDatasets([]);
    setMetrics([]);
    setEndpoints([]);
    setFormError(null);
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
          <div>
            <h1 className="text-2xl font-bold text-gray-900">All Evaluations</h1>
            <p className="mt-1 text-sm text-gray-500">
              View and create evaluation runs across all your projects
            </p>
          </div>
          <Button onClick={() => setIsNewModalOpen(true)}>
            <Plus className="h-4 w-4 mr-2" />
            New Evaluation
          </Button>
        </div>

        {error && <Alert variant="error">{error}</Alert>}

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
                    Project
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
                    <td className="px-6 py-4">
                      <Link
                        href={`/projects/${evaluation.projectId}`}
                        className="flex items-center gap-2 text-sm text-gray-600 hover:text-primary-600"
                      >
                        <FolderKanban className="h-4 w-4" />
                        {evaluation.projectName}
                      </Link>
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
                        <div className="flex-1 bg-gray-200 rounded-full h-2 w-20">
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
                Create your first evaluation to measure LLM performance
              </p>
              <Button onClick={() => setIsNewModalOpen(true)}>
                <Plus className="h-4 w-4 mr-2" />
                New Evaluation
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* New Evaluation Modal */}
      <Modal
        isOpen={isNewModalOpen}
        onClose={() => {
          setIsNewModalOpen(false);
          resetForm();
        }}
        title="New Evaluation"
        size="lg"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}

          {/* Step 1: Select Project */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Project <span className="text-red-500">*</span>
            </label>
            <select
              value={formData.projectId}
              onChange={(e) => handleProjectChange(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="">Select a project...</option>
              {projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          </div>

          {formData.projectId && (
            <>
              {loadingFormData ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin text-primary-600" />
                  <span className="ml-2 text-sm text-gray-500">Loading project data...</span>
                </div>
              ) : (
                <>
                  <Input
                    label="Evaluation Name"
                    value={formData.name}
                    onChange={(e) =>
                      setFormData({ ...formData, name: e.target.value })
                    }
                    placeholder="e.g., Q4 RAG Performance Test"
                    required
                  />

                  {/* Step 2: Select Dataset */}
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
                    >
                      <option value="">Select a dataset...</option>
                      {datasets.map((dataset) => (
                        <option key={dataset.id} value={dataset.id}>
                          {dataset.name} ({dataset.testCaseCount} test cases)
                        </option>
                      ))}
                    </select>
                    {datasets.length === 0 && (
                      <p className="mt-1 text-xs text-yellow-600">
                        No datasets found. Create a dataset first.
                      </p>
                    )}
                  </div>

                  {/* Step 3: Select Judge LLM */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Judge LLM
                    </label>
                    <p className="text-xs text-gray-500 mb-2">
                      Used to evaluate outputs with LLM-as-Judge metrics (coherence, relevance, etc.)
                    </p>
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
                    {endpoints.length === 0 && (
                      <p className="mt-1 text-xs text-yellow-600">
                        No LLM endpoints found. Add an endpoint to use LLM-as-Judge metrics.
                      </p>
                    )}
                  </div>

                  {/* Step 4: Select Metrics */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Metrics <span className="text-red-500">*</span>
                    </label>
                    {metrics.length > 0 ? (
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
                    ) : (
                      <p className="text-sm text-gray-500">No metrics available.</p>
                    )}
                    <p className="mt-1 text-xs text-gray-500">
                      Selected: {formData.selectedMetrics.length} metric(s)
                    </p>
                  </div>
                </>
              )}
            </>
          )}

          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => {
                setIsNewModalOpen(false);
                resetForm();
              }}
            >
              Cancel
            </Button>
            <Button
              onClick={handleStartEvaluation}
              loading={submitting}
              disabled={!formData.projectId || loadingFormData}
            >
              <Play className="h-4 w-4 mr-2" />
              Start Evaluation
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
