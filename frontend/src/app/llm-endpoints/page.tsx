"use client";

import { useState, useEffect, useCallback, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, Alert, Modal, Input } from "@/components/ui";
import { api } from "@/lib/api";
import {
  Project,
  LlmEndpointSummary,
  PagedResponse,
  ProviderType,
  AuthType,
  LlmType,
  CreateLlmEndpointRequest,
} from "@/lib/types";
import {
  Cpu,
  Star,
  FolderKanban,
  Plus,
  Eye,
  EyeOff,
  Loader2,
  Bot,
  Scale,
  Info,
} from "lucide-react";

const PROVIDER_OPTIONS: { value: ProviderType; label: string }[] = [
  { value: "OPENAI", label: "OpenAI" },
  { value: "ANTHROPIC", label: "Anthropic" },
  { value: "AZURE_OPENAI", label: "Azure OpenAI" },
  { value: "AWS_BEDROCK", label: "AWS Bedrock" },
  { value: "GOOGLE_VERTEX", label: "Google Vertex AI" },
  { value: "CUSTOM", label: "Custom" },
];

const AUTH_TYPE_OPTIONS: { value: AuthType; label: string; description: string }[] = [
  { value: "API_KEY", label: "API Key", description: "Standard API key authentication" },
  { value: "BEARER", label: "Bearer Token", description: "Bearer token in Authorization header" },
  { value: "BASIC", label: "Basic Auth", description: "Username and password (base64 encoded)" },
  { value: "NONE", label: "No Auth", description: "No authentication required" },
];

const MODEL_OPTIONS: Record<ProviderType, string[]> = {
  OPENAI: ["gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo"],
  ANTHROPIC: ["claude-3-5-sonnet-latest", "claude-3-5-haiku-latest", "claude-3-opus-latest"],
  AZURE_OPENAI: ["gpt-4o", "gpt-4", "gpt-35-turbo"],
  AWS_BEDROCK: ["anthropic.claude-3-sonnet", "anthropic.claude-3-haiku", "amazon.titan-text-express"],
  GOOGLE_VERTEX: ["gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.0-pro"],
  CUSTOM: [],
};

function AllLlmEndpointsPageInner() {
  const searchParams = useSearchParams();
  const llmType = searchParams.get("type"); // "client" or "judge"

  const [endpoints, setEndpoints] = useState<LlmEndpointSummary[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const getPageInfo = () => {
    if (llmType === "client") {
      return {
        title: "Client LLM Endpoints",
        subtitle: "LLMs being tested - these receive questions and generate responses",
        icon: Bot,
        color: "blue",
      };
    } else if (llmType === "judge") {
      return {
        title: "Judge LLM Endpoints",
        subtitle: "LLMs that evaluate responses - used for quality assessment metrics",
        icon: Scale,
        color: "purple",
      };
    }
    return {
      title: "All LLM Endpoints",
      subtitle: "View and manage LLM endpoints across all your projects",
      icon: Cpu,
      color: "gray",
    };
  };

  const pageInfo = getPageInfo();

  // New Endpoint Modal
  const [isNewModalOpen, setIsNewModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [showApiKey, setShowApiKey] = useState(false);

  // Derive llmType from URL param (convert to uppercase for API)
  const llmTypeApi: LlmType | null = llmType === "client" ? "CLIENT" : llmType === "judge" ? "JUDGE" : null;

  // Form state
  const [formData, setFormData] = useState<CreateLlmEndpointRequest & { projectId: string }>({
    projectId: "",
    name: "",
    description: "",
    providerType: "OPENAI",
    modelName: "gpt-4o",
    apiUrl: "",
    authType: "API_KEY",
    apiKey: "",
    llmType: llmTypeApi || "CLIENT",
    isDefault: false,
  });

  const fetchAllData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      // Fetch projects for the create modal
      const projectsRes = await api.get<PagedResponse<Project>>(
        "/projects?size=100"
      );

      if (!projectsRes.success || !projectsRes.data) {
        setError("Failed to fetch projects");
        return;
      }

      setProjects(projectsRes.data.content);

      // If llmType is specified, use the global endpoint API
      if (llmTypeApi) {
        const endpointsRes = await api.get<PagedResponse<LlmEndpointSummary>>(
          `/llm-endpoints?type=${llmTypeApi}&size=100`
        );

        if (endpointsRes.success && endpointsRes.data) {
          const sortedEndpoints = [...endpointsRes.data.content].sort(
            (a, b) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
          setEndpoints(sortedEndpoints);
        }
      } else {
        // Fetch all endpoints across all projects
        const allEndpoints: LlmEndpointSummary[] = [];

        const fetchPromises = projectsRes.data.content.map(async (project) => {
          const endpointsRes = await api.get<PagedResponse<LlmEndpointSummary>>(
            `/projects/${project.id}/llm-endpoints?size=100`
          );
          if (endpointsRes.success && endpointsRes.data) {
            allEndpoints.push(...endpointsRes.data.content);
          }
        });

        await Promise.all(fetchPromises);

        allEndpoints.sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );

        setEndpoints(allEndpoints);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [llmTypeApi]);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  const handleCreate = async () => {
    if (!formData.projectId) {
      setFormError("Please select a project");
      return;
    }
    if (!formData.name.trim()) {
      setFormError("Name is required");
      return;
    }
    if (formData.authType !== "NONE" && !formData.apiKey.trim()) {
      setFormError("API Key/Token is required for this auth type");
      return;
    }
    if (formData.providerType === "CUSTOM" && !formData.apiUrl?.trim()) {
      setFormError("API URL is required for custom providers");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const { projectId, ...endpointData } = formData;
      const response = await api.post(`/projects/${projectId}/llm-endpoints`, endpointData);
      if (response.success) {
        setIsNewModalOpen(false);
        resetForm();
        fetchAllData();
      } else {
        setFormError(response.error || "Failed to create LLM endpoint");
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
      description: "",
      providerType: "OPENAI",
      modelName: "gpt-4o",
      apiUrl: "",
      authType: "API_KEY",
      apiKey: "",
      llmType: llmTypeApi || "CLIENT",
      isDefault: false,
    });
    setShowApiKey(false);
    setFormError(null);
  };

  const getProviderBadgeVariant = (provider: ProviderType) => {
    switch (provider) {
      case "OPENAI":
        return "success";
      case "ANTHROPIC":
        return "warning";
      case "AZURE_OPENAI":
        return "info";
      default:
        return "default";
    }
  };

  const getAuthLabel = () => {
    switch (formData.authType) {
      case "API_KEY":
        return "API Key";
      case "BEARER":
        return "Bearer Token";
      case "BASIC":
        return "Credentials (username:password)";
      default:
        return "API Key";
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-start gap-3">
            <div className={`h-10 w-10 rounded-lg flex items-center justify-center ${
              llmType === "client" ? "bg-blue-100" :
              llmType === "judge" ? "bg-purple-100" : "bg-gray-100"
            }`}>
              <pageInfo.icon className={`h-5 w-5 ${
                llmType === "client" ? "text-blue-600" :
                llmType === "judge" ? "text-purple-600" : "text-gray-600"
              }`} />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{pageInfo.title}</h1>
              <p className="mt-1 text-sm text-gray-500">{pageInfo.subtitle}</p>
            </div>
          </div>
          <Button onClick={() => setIsNewModalOpen(true)}>
            <Plus className="h-4 w-4 mr-2" />
            New Endpoint
          </Button>
        </div>

        {/* Info Box */}
        {llmType && (
          <div className={`p-4 rounded-lg border ${
            llmType === "client"
              ? "bg-blue-50 border-blue-200"
              : "bg-purple-50 border-purple-200"
          }`}>
            <div className="flex items-start gap-3">
              <Info className={`h-5 w-5 mt-0.5 ${
                llmType === "client" ? "text-blue-600" : "text-purple-600"
              }`} />
              <div className="text-sm">
                {llmType === "client" ? (
                  <>
                    <p className="font-medium text-blue-900 mb-1">What is a Client LLM?</p>
                    <p className="text-blue-700">
                      The Client LLM (also called Target LLM) is the LLM you are testing. During evaluation,
                      questions from test cases are sent to this LLM, and it generates responses that will be evaluated.
                    </p>
                    <p className="text-blue-700 mt-2">
                      <strong>Example:</strong> Your chatbot, RAG system, or AI agent that you want to evaluate.
                    </p>
                  </>
                ) : (
                  <>
                    <p className="font-medium text-purple-900 mb-1">What is a Judge LLM?</p>
                    <p className="text-purple-700">
                      The Judge LLM evaluates the quality of responses. It&apos;s used for LLM-as-Judge metrics
                      like Faithfulness, Relevance, and Answer Correctness.
                    </p>
                    <p className="text-purple-700 mt-2">
                      <strong>Example:</strong> GPT-4 or Claude evaluating whether responses are accurate and relevant.
                    </p>
                  </>
                )}
              </div>
            </div>
          </div>
        )}

        {error && <Alert variant="error">{error}</Alert>}

        {/* Endpoints List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : endpoints.length > 0 ? (
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Name
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Project
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Provider
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Model
                  </th>
                  {!llmType && (
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                      Type
                    </th>
                  )}
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Status
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Created
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {endpoints.map((endpoint) => (
                  <tr key={endpoint.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <Cpu className="h-4 w-4 text-gray-400" />
                        <span className="font-medium text-gray-900">
                          {endpoint.name}
                        </span>
                        {endpoint.isDefault && (
                          <Star className="h-4 w-4 text-yellow-500 fill-yellow-500" />
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <Link
                        href={`/projects/${endpoint.projectId}`}
                        className="flex items-center gap-2 text-sm text-gray-600 hover:text-primary-600"
                      >
                        <FolderKanban className="h-4 w-4" />
                        {endpoint.projectName}
                      </Link>
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={getProviderBadgeVariant(endpoint.providerType)}>
                        {endpoint.providerType}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {endpoint.modelName}
                    </td>
                    {!llmType && (
                      <td className="px-6 py-4">
                        <Badge variant={endpoint.llmType === "CLIENT" ? "info" : "default"}>
                          {endpoint.llmType === "CLIENT" ? (
                            <span className="flex items-center gap-1">
                              <Bot className="h-3 w-3" />
                              Client
                            </span>
                          ) : (
                            <span className="flex items-center gap-1">
                              <Scale className="h-3 w-3" />
                              Judge
                            </span>
                          )}
                        </Badge>
                      </td>
                    )}
                    <td className="px-6 py-4">
                      <Badge variant={endpoint.isActive ? "success" : "warning"}>
                        {endpoint.isActive ? "Active" : "Inactive"}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {new Date(endpoint.createdAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="px-6 py-12 text-center">
              <Cpu className="h-12 w-12 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-1">
                No LLM endpoints yet
              </h3>
              <p className="text-sm text-gray-500 mb-4">
                Create your first LLM endpoint to start running evaluations
              </p>
              <Button onClick={() => setIsNewModalOpen(true)}>
                <Plus className="h-4 w-4 mr-2" />
                New Endpoint
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* New Endpoint Modal */}
      <Modal
        isOpen={isNewModalOpen}
        onClose={() => {
          setIsNewModalOpen(false);
          resetForm();
        }}
        title="New LLM Endpoint"
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
              onChange={(e) => setFormData({ ...formData, projectId: e.target.value })}
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
              <Input
                label="Name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="e.g., Production GPT-4"
                required
              />

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Optional description..."
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
                  rows={2}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Provider <span className="text-red-500">*</span>
                </label>
                <select
                  value={formData.providerType}
                  onChange={(e) => {
                    const provider = e.target.value as ProviderType;
                    setFormData({
                      ...formData,
                      providerType: provider,
                      modelName: MODEL_OPTIONS[provider][0] || "",
                      authType: provider === "CUSTOM" ? formData.authType : "API_KEY",
                    });
                  }}
                  className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
                >
                  {PROVIDER_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Model <span className="text-red-500">*</span>
                </label>
                {MODEL_OPTIONS[formData.providerType].length > 0 ? (
                  <select
                    value={formData.modelName}
                    onChange={(e) => setFormData({ ...formData, modelName: e.target.value })}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
                  >
                    {MODEL_OPTIONS[formData.providerType].map((model) => (
                      <option key={model} value={model}>
                        {model}
                      </option>
                    ))}
                  </select>
                ) : (
                  <Input
                    value={formData.modelName}
                    onChange={(e) => setFormData({ ...formData, modelName: e.target.value })}
                    placeholder="Enter model name or identifier"
                  />
                )}
              </div>

              {/* Custom Provider Fields */}
              {formData.providerType === "CUSTOM" && (
                <>
                  <Input
                    label="API URL"
                    value={formData.apiUrl || ""}
                    onChange={(e) => setFormData({ ...formData, apiUrl: e.target.value })}
                    placeholder="https://api.example.com/v1/chat/completions"
                    required
                  />

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Authentication Type <span className="text-red-500">*</span>
                    </label>
                    <select
                      value={formData.authType}
                      onChange={(e) => setFormData({ ...formData, authType: e.target.value as AuthType })}
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
                    >
                      {AUTH_TYPE_OPTIONS.map((opt) => (
                        <option key={opt.value} value={opt.value}>
                          {opt.label} - {opt.description}
                        </option>
                      ))}
                    </select>
                  </div>
                </>
              )}

              {/* API Key / Token Input */}
              {formData.authType !== "NONE" && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    {getAuthLabel()} <span className="text-red-500">*</span>
                  </label>
                  <div className="relative">
                    <input
                      type={showApiKey ? "text" : "password"}
                      value={formData.apiKey}
                      onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
                      placeholder={
                        formData.authType === "BASIC"
                          ? "username:password"
                          : `Enter ${getAuthLabel().toLowerCase()}`
                      }
                      className="w-full rounded-lg border border-gray-300 px-3 py-2 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
                      required
                    />
                    <button
                      type="button"
                      onClick={() => setShowApiKey(!showApiKey)}
                      className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600"
                    >
                      {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                  {formData.authType === "BASIC" && (
                    <p className="mt-1 text-xs text-gray-500">
                      Enter credentials in format: username:password
                    </p>
                  )}
                </div>
              )}

              {/* LLM Type Selection */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  LLM Type <span className="text-red-500">*</span>
                </label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setFormData({ ...formData, llmType: "CLIENT" })}
                    className={`p-3 rounded-lg border-2 text-left transition-all ${
                      formData.llmType === "CLIENT"
                        ? "border-blue-500 bg-blue-50"
                        : "border-gray-200 hover:border-gray-300"
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <Bot className={`h-4 w-4 ${formData.llmType === "CLIENT" ? "text-blue-600" : "text-gray-400"}`} />
                      <span className={`font-medium ${formData.llmType === "CLIENT" ? "text-blue-900" : "text-gray-700"}`}>
                        Client LLM
                      </span>
                    </div>
                    <p className={`text-xs ${formData.llmType === "CLIENT" ? "text-blue-700" : "text-gray-500"}`}>
                      LLM being tested (generates responses)
                    </p>
                  </button>
                  <button
                    type="button"
                    onClick={() => setFormData({ ...formData, llmType: "JUDGE" })}
                    className={`p-3 rounded-lg border-2 text-left transition-all ${
                      formData.llmType === "JUDGE"
                        ? "border-purple-500 bg-purple-50"
                        : "border-gray-200 hover:border-gray-300"
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <Scale className={`h-4 w-4 ${formData.llmType === "JUDGE" ? "text-purple-600" : "text-gray-400"}`} />
                      <span className={`font-medium ${formData.llmType === "JUDGE" ? "text-purple-900" : "text-gray-700"}`}>
                        Judge LLM
                      </span>
                    </div>
                    <p className={`text-xs ${formData.llmType === "JUDGE" ? "text-purple-700" : "text-gray-500"}`}>
                      LLM that evaluates responses
                    </p>
                  </button>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="isDefault"
                  checked={formData.isDefault}
                  onChange={(e) => setFormData({ ...formData, isDefault: e.target.checked })}
                  className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                />
                <label htmlFor="isDefault" className="text-sm text-gray-700">
                  Set as default endpoint for this project
                </label>
              </div>
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
              onClick={handleCreate}
              loading={submitting}
              disabled={!formData.projectId}
            >
              <Plus className="h-4 w-4 mr-2" />
              Create Endpoint
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}

export default function AllLlmEndpointsPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600" /></div>}>
      <AllLlmEndpointsPageInner />
    </Suspense>
  );
}
