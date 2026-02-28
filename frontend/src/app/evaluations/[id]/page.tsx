"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import {
  EvaluationRun,
  EvaluationResult,
  PagedResponse,
  EvaluationStatus,
  MetricSummary,
} from "@/lib/types";
import {
  ArrowLeft,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Loader2,
  BarChart3,
  FileText,
  Timer,
  DollarSign,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  StopCircle,
  RotateCcw,
  Trash2,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";

export default function EvaluationDetailPage() {
  const params = useParams();
  const router = useRouter();
  const { user } = useAuth();
  const evaluationId = params.id as string;

  const [evaluation, setEvaluation] = useState<EvaluationRun | null>(null);
  const [results, setResults] = useState<EvaluationResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [resultsLoading, setResultsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedResult, setExpandedResult] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"overview" | "results">("overview");
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const fetchEvaluation = useCallback(async () => {
    try {
      const response = await api.get<EvaluationRun>(`/evaluations/${evaluationId}`);
      if (response.success && response.data) {
        setEvaluation(response.data);
      } else {
        setError(response.error || "Failed to fetch evaluation");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [evaluationId]);

  const fetchResults = useCallback(async () => {
    setResultsLoading(true);
    try {
      const response = await api.get<PagedResponse<EvaluationResult>>(
        `/evaluations/${evaluationId}/results?size=100`
      );
      if (response.success && response.data) {
        setResults(response.data.content);
      }
    } catch (err) {
      console.error("Failed to fetch results", err);
    } finally {
      setResultsLoading(false);
    }
  }, [evaluationId]);

  useEffect(() => {
    fetchEvaluation();
  }, [fetchEvaluation]);

  useEffect(() => {
    if (evaluation && activeTab === "results") {
      fetchResults();
    }
  }, [evaluation, activeTab, fetchResults]);

  // Auto-refresh for running evaluations
  useEffect(() => {
    if (evaluation?.status === "RUNNING" || evaluation?.status === "PENDING") {
      const interval = setInterval(() => {
        fetchEvaluation();
        if (activeTab === "results") {
          fetchResults();
        }
      }, 5000);
      return () => clearInterval(interval);
    }
  }, [evaluation?.status, activeTab, fetchEvaluation, fetchResults]);

  const handleCancel = async () => {
    if (!confirm("Are you sure you want to cancel this evaluation?")) {
      return;
    }

    setActionLoading("cancel");
    try {
      const response = await api.post(`/evaluations/${evaluationId}/cancel`);
      if (response.success) {
        fetchEvaluation();
      } else {
        setError(response.error || "Failed to cancel evaluation");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setActionLoading(null);
    }
  };

  const handleDelete = async () => {
    if (!confirm("Are you sure you want to permanently delete this evaluation? This action cannot be undone.")) {
      return;
    }

    setActionLoading("delete");
    try {
      const response = await api.delete(`/evaluations/${evaluationId}`);
      if (response.success) {
        router.push("/evaluations");
      } else {
        setError(response.error || "Failed to delete evaluation");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setActionLoading(null);
    }
  };

  const handleRetrigger = async () => {
    if (!confirm("This will create a new evaluation run with the same configuration. Continue?")) {
      return;
    }

    setActionLoading("retrigger");
    try {
      const response = await api.post<EvaluationRun>(`/evaluations/${evaluationId}/retrigger`);
      if (response.success && response.data) {
        router.push(`/evaluations/${response.data.id}`);
      } else {
        setError(response.error || "Failed to retrigger evaluation");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setActionLoading(null);
    }
  };

  const getStatusIcon = (status: EvaluationStatus) => {
    switch (status) {
      case "PENDING":
        return <Clock className="h-5 w-5 text-gray-500" />;
      case "RUNNING":
        return <Loader2 className="h-5 w-5 text-blue-500 animate-spin" />;
      case "COMPLETED":
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case "FAILED":
        return <XCircle className="h-5 w-5 text-red-500" />;
      case "CANCELLED":
        return <AlertCircle className="h-5 w-5 text-yellow-500" />;
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

  const getScoreColor = (score: number) => {
    if (score >= 0.8) return "text-green-600";
    if (score >= 0.6) return "text-yellow-600";
    return "text-red-600";
  };

  const getScoreBgColor = (score: number) => {
    if (score >= 0.8) return "bg-green-100";
    if (score >= 0.6) return "bg-yellow-100";
    return "bg-red-100";
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      </DashboardLayout>
    );
  }

  if (error || !evaluation) {
    return (
      <DashboardLayout>
        <div className="space-y-4">
          <Button variant="ghost" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back
          </Button>
          <Alert variant="error">{error || "Evaluation not found"}</Alert>
        </div>
      </DashboardLayout>
    );
  }

  const progress =
    evaluation.totalTestCases > 0
      ? (evaluation.completedTestCases / evaluation.totalTestCases) * 100
      : 0;

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
              <div className="flex items-center gap-3">
                {getStatusIcon(evaluation.status)}
                <h1 className="text-2xl font-bold text-gray-900">
                  {evaluation.name}
                </h1>
                <Badge variant={getStatusBadgeVariant(evaluation.status)}>
                  {evaluation.status}
                </Badge>
              </div>
              <p className="mt-1 text-sm text-gray-500">
                Run #{evaluation.runNumber} · {evaluation.projectName} ·{" "}
                {evaluation.datasetName}
              </p>
              <p className="mt-1 text-xs text-gray-400">
                Started by {evaluation.createdByName} on{" "}
                {new Date(evaluation.createdAt).toLocaleString()}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {(evaluation.status === "RUNNING" ||
              evaluation.status === "PENDING") && (
              <>
                <Button variant="outline" size="sm" onClick={fetchEvaluation}>
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Refresh
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleCancel}
                  loading={actionLoading === "cancel"}
                  className="text-red-600 border-red-300 hover:bg-red-50"
                >
                  <StopCircle className="h-4 w-4 mr-2" />
                  Cancel
                </Button>
              </>
            )}
            {(evaluation.status === "COMPLETED" ||
              evaluation.status === "FAILED" ||
              evaluation.status === "CANCELLED") && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleRetrigger}
                loading={actionLoading === "retrigger"}
              >
                <RotateCcw className="h-4 w-4 mr-2" />
                Retrigger
              </Button>
            )}
            {user?.role === "ADMIN" &&
              evaluation.status !== "RUNNING" &&
              evaluation.status !== "PENDING" && (
              <Button
                variant="outline"
                size="sm"
                onClick={handleDelete}
                loading={actionLoading === "delete"}
                className="text-red-600 border-red-300 hover:bg-red-50"
              >
                <Trash2 className="h-4 w-4 mr-2" />
                Delete
              </Button>
            )}
          </div>
        </div>

        {evaluation.errorMessage && (
          <Alert variant="error">{evaluation.errorMessage}</Alert>
        )}

        {/* Progress Bar (for running evaluations) */}
        {(evaluation.status === "RUNNING" || evaluation.status === "PENDING") && (
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700">Progress</span>
              <span className="text-sm text-gray-500">
                {evaluation.completedTestCases} / {evaluation.totalTestCases} test
                cases
              </span>
            </div>
            <div className="bg-gray-200 rounded-full h-3">
              <div
                className="bg-primary-600 h-3 rounded-full transition-all duration-500"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-blue-100 flex items-center justify-center">
                <BarChart3 className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p
                  className={`text-2xl font-bold ${
                    evaluation.overallScore !== undefined
                      ? getScoreColor(evaluation.overallScore)
                      : "text-gray-400"
                  }`}
                >
                  {evaluation.overallScore !== undefined
                    ? `${(evaluation.overallScore * 100).toFixed(1)}%`
                    : "-"}
                </p>
                <p className="text-sm text-gray-500">Overall Score</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-green-100 flex items-center justify-center">
                <FileText className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {evaluation.completedTestCases}
                  <span className="text-gray-400 text-lg">
                    /{evaluation.totalTestCases}
                  </span>
                </p>
                <p className="text-sm text-gray-500">Test Cases</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-purple-100 flex items-center justify-center">
                <Timer className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {evaluation.totalTokens?.toLocaleString() || "-"}
                </p>
                <p className="text-sm text-gray-500">Total Tokens</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-yellow-100 flex items-center justify-center">
                <DollarSign className="h-5 w-5 text-yellow-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {evaluation.totalCost !== undefined
                    ? `$${evaluation.totalCost.toFixed(4)}`
                    : "-"}
                </p>
                <p className="text-sm text-gray-500">Estimated Cost</p>
              </div>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200">
          <nav className="flex gap-4">
            <button
              onClick={() => setActiveTab("overview")}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeTab === "overview"
                  ? "border-primary-600 text-primary-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              Overview
            </button>
            <button
              onClick={() => setActiveTab("results")}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeTab === "results"
                  ? "border-primary-600 text-primary-600"
                  : "border-transparent text-gray-500 hover:text-gray-700"
              }`}
            >
              Results ({evaluation.completedTestCases})
            </button>
          </nav>
        </div>

        {/* Tab Content */}
        {activeTab === "overview" ? (
          <div className="space-y-6">
            {/* Metric Summaries */}
            {evaluation.metricSummaries &&
              Object.keys(evaluation.metricSummaries).length > 0 && (
                <div className="bg-white rounded-xl border border-gray-200">
                  <div className="px-6 py-4 border-b border-gray-200">
                    <h2 className="text-lg font-semibold text-gray-900">
                      Metric Scores
                    </h2>
                  </div>
                  <div className="p-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                      {Object.entries(evaluation.metricSummaries).map(
                        ([code, summary]: [string, MetricSummary]) => (
                          <div
                            key={code}
                            className="border border-gray-200 rounded-lg p-4"
                          >
                            <div className="flex items-center justify-between mb-2">
                              <h3 className="font-medium text-gray-900">
                                {summary.metricName}
                              </h3>
                              <span
                                className={`text-lg font-bold ${getScoreColor(
                                  summary.averageScore
                                )}`}
                              >
                                {(summary.averageScore * 100).toFixed(1)}%
                              </span>
                            </div>
                            <div className="bg-gray-200 rounded-full h-2 mb-3">
                              <div
                                className={`h-2 rounded-full transition-all ${getScoreBgColor(
                                  summary.averageScore
                                ).replace("100", "500")}`}
                                style={{
                                  width: `${summary.averageScore * 100}%`,
                                }}
                              />
                            </div>
                            <div className="grid grid-cols-3 gap-2 text-xs text-gray-500">
                              <div>
                                <span className="block text-gray-400">Min</span>
                                {(summary.minScore * 100).toFixed(1)}%
                              </div>
                              <div>
                                <span className="block text-gray-400">Max</span>
                                {(summary.maxScore * 100).toFixed(1)}%
                              </div>
                              <div>
                                <span className="block text-gray-400">
                                  Std Dev
                                </span>
                                {(summary.stdDev * 100).toFixed(1)}%
                              </div>
                            </div>
                            {summary.passRate !== undefined && (
                              <div className="mt-2 pt-2 border-t border-gray-100 text-xs">
                                <span className="text-gray-400">Pass Rate: </span>
                                <span className="font-medium">
                                  {(summary.passRate * 100).toFixed(0)}%
                                </span>
                                <span className="text-gray-400">
                                  {" "}
                                  ({summary.totalEvaluated} evaluated)
                                </span>
                              </div>
                            )}
                          </div>
                        )
                      )}
                    </div>
                  </div>
                </div>
              )}

            {/* Configuration */}
            <div className="bg-white rounded-xl border border-gray-200">
              <div className="px-6 py-4 border-b border-gray-200">
                <h2 className="text-lg font-semibold text-gray-900">
                  Configuration
                </h2>
              </div>
              <div className="p-6">
                <dl className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <dt className="text-sm text-gray-500">Evaluation Mode</dt>
                    <dd className="text-sm font-medium text-gray-900">
                      {evaluation.evaluationMode === "PRE_GENERATED"
                        ? "Pre-generated"
                        : "Live Generation"}
                    </dd>
                    <dd className="text-xs text-gray-400">
                      {evaluation.evaluationMode === "PRE_GENERATED"
                        ? "LLM outputs provided in test cases"
                        : "Client LLM generates outputs at runtime"}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-sm text-gray-500">Trigger Type</dt>
                    <dd className="text-sm font-medium text-gray-900">
                      {evaluation.triggerType}
                    </dd>
                  </div>
                  {evaluation.targetLlmEndpointName && (
                    <div>
                      <dt className="text-sm text-gray-500">Client LLM</dt>
                      <dd className="text-sm font-medium text-gray-900">
                        {evaluation.targetLlmEndpointName}
                      </dd>
                      <dd className="text-xs text-gray-400">
                        Receives questions and generates responses
                      </dd>
                    </div>
                  )}
                  {evaluation.judgeLlmEndpointName && (
                    <div>
                      <dt className="text-sm text-gray-500">Judge LLM</dt>
                      <dd className="text-sm font-medium text-gray-900">
                        {evaluation.judgeLlmEndpointName}
                      </dd>
                      <dd className="text-xs text-gray-400">
                        Evaluates quality of responses
                      </dd>
                    </div>
                  )}
                </dl>

                {evaluation.metrics && evaluation.metrics.length > 0 && (
                  <div className="mt-6 pt-6 border-t border-gray-200">
                    <h3 className="text-sm font-medium text-gray-900 mb-3">
                      Selected Metrics
                    </h3>
                    <div className="flex flex-wrap gap-2">
                      {evaluation.metrics.map((metric) => (
                        <Badge key={metric.metricId} variant="info">
                          {metric.metricName}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-gray-200">
            {resultsLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
              </div>
            ) : results.length > 0 ? (
              <div className="divide-y divide-gray-200">
                {results.map((result) => (
                  <div key={result.id} className="p-4">
                    <div
                      className="flex items-start justify-between cursor-pointer"
                      onClick={() =>
                        setExpandedResult(
                          expandedResult === result.id ? null : result.id
                        )
                      }
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          {result.status === "COMPLETED" ? (
                            <CheckCircle className="h-4 w-4 text-green-500" />
                          ) : result.status === "FAILED" ? (
                            <XCircle className="h-4 w-4 text-red-500" />
                          ) : (
                            <Clock className="h-4 w-4 text-gray-400" />
                          )}
                          <p className="font-medium text-gray-900 line-clamp-1">
                            {result.question}
                          </p>
                        </div>
                        <div className="mt-1 flex items-center gap-4 text-xs text-gray-500">
                          {result.llmLatencyMs && (
                            <span>Latency: {result.llmLatencyMs}ms</span>
                          )}
                          {result.inputTokens !== undefined && (
                            <span>
                              Tokens: {result.inputTokens} in /{" "}
                              {result.outputTokens} out
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-4">
                        {result.scores.length > 0 && (
                          <div className="flex items-center gap-2">
                            {result.scores.slice(0, 3).map((score) => (
                              <div
                                key={score.metricCode}
                                className={`px-2 py-1 rounded text-xs font-medium ${getScoreBgColor(
                                  score.score
                                )} ${getScoreColor(score.score)}`}
                                title={score.metricName}
                              >
                                {(score.score * 100).toFixed(0)}%
                              </div>
                            ))}
                            {result.scores.length > 3 && (
                              <span className="text-xs text-gray-400">
                                +{result.scores.length - 3}
                              </span>
                            )}
                          </div>
                        )}
                        {expandedResult === result.id ? (
                          <ChevronUp className="h-5 w-5 text-gray-400" />
                        ) : (
                          <ChevronDown className="h-5 w-5 text-gray-400" />
                        )}
                      </div>
                    </div>

                    {expandedResult === result.id && (
                      <div className="mt-4 pt-4 border-t border-gray-100 space-y-4">
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div>
                            <h4 className="text-xs font-medium text-gray-500 uppercase mb-1">
                              Question
                            </h4>
                            <p className="text-sm text-gray-900 bg-gray-50 p-3 rounded-lg">
                              {result.question}
                            </p>
                          </div>
                          {result.groundTruth && (
                            <div>
                              <h4 className="text-xs font-medium text-gray-500 uppercase mb-1">
                                Ground Truth
                              </h4>
                              <p className="text-sm text-gray-900 bg-gray-50 p-3 rounded-lg">
                                {result.groundTruth}
                              </p>
                            </div>
                          )}
                        </div>

                        {(result.llmOutput || result.generatedOutput) && (
                          <div>
                            <h4 className="text-xs font-medium text-gray-500 uppercase mb-1">
                              LLM Output
                            </h4>
                            <p className="text-sm text-gray-900 bg-gray-50 p-3 rounded-lg whitespace-pre-wrap">
                              {result.generatedOutput || result.llmOutput}
                            </p>
                          </div>
                        )}

                        {result.scores.length > 0 && (
                          <div>
                            <h4 className="text-xs font-medium text-gray-500 uppercase mb-2">
                              Metric Scores
                            </h4>
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                              {result.scores.map((score) => (
                                <div
                                  key={score.metricCode}
                                  className="border border-gray-200 rounded-lg p-3"
                                >
                                  <div className="flex items-center justify-between mb-1">
                                    <span className="text-sm font-medium text-gray-900">
                                      {score.metricName}
                                    </span>
                                    <span
                                      className={`text-sm font-bold ${getScoreColor(
                                        score.score
                                      )}`}
                                    >
                                      {(score.score * 100).toFixed(1)}%
                                    </span>
                                  </div>
                                  {score.passed !== undefined && (
                                    <Badge
                                      variant={
                                        score.passed ? "success" : "error"
                                      }
                                      className="text-xs"
                                    >
                                      {score.passed ? "Passed" : "Failed"}
                                    </Badge>
                                  )}
                                  {score.judgeReasoning && (
                                    <p className="mt-2 text-xs text-gray-500">
                                      {score.judgeReasoning}
                                    </p>
                                  )}
                                  {score.errorMessage && (
                                    <p className="mt-2 text-xs text-red-500">
                                      {score.errorMessage}
                                    </p>
                                  )}
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {result.errorMessage && (
                          <Alert variant="error">{result.errorMessage}</Alert>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="px-6 py-12 text-center text-gray-500">
                No results available yet.
              </div>
            )}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
