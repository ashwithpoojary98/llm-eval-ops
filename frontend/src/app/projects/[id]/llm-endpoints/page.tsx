"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, Alert, Modal, Input } from "@/components/ui";
import { api } from "@/lib/api";
import { toast } from "sonner";
import {
  LlmEndpoint,
  LlmEndpointSummary,
  PagedResponse,
  ProviderType,
  AuthType,
  CreateLlmEndpointRequest,
} from "@/lib/types";
import {
  ArrowLeft,
  Plus,
  Cpu,
  Edit,
  Trash2,
  Star,
  Eye,
  EyeOff,
} from "lucide-react";

const PROVIDER_OPTIONS: { value: ProviderType; label: string }[] = [
  { value: "OPENAI", label: "OpenAI" },
  { value: "ANTHROPIC", label: "Anthropic" },
  { value: "AZURE_OPENAI", label: "Azure OpenAI" },
  { value: "AWS_BEDROCK", label: "AWS Bedrock" },
  { value: "GOOGLE_VERTEX", label: "Google Vertex AI" },
  { value: "CUSTOM", label: "Custom" },
];

const MODEL_OPTIONS: Record<ProviderType, string[]> = {
  OPENAI: ["gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "gpt-4", "gpt-3.5-turbo"],
  ANTHROPIC: ["claude-3-5-sonnet-latest", "claude-3-5-haiku-latest", "claude-3-opus-latest"],
  AZURE_OPENAI: ["gpt-4o", "gpt-4", "gpt-35-turbo"],
  AWS_BEDROCK: ["anthropic.claude-3-sonnet", "anthropic.claude-3-haiku", "amazon.titan-text-express"],
  GOOGLE_VERTEX: ["gemini-1.5-pro", "gemini-1.5-flash", "gemini-1.0-pro"],
  CUSTOM: [],
};

export default function LlmEndpointsPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const [endpoints, setEndpoints] = useState<LlmEndpointSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [includeInactive, setIncludeInactive] = useState(false);

  // Modal state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingEndpoint, setEditingEndpoint] = useState<LlmEndpoint | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [showApiKey, setShowApiKey] = useState(false);

  // Form state
  const [formData, setFormData] = useState<CreateLlmEndpointRequest>({
    name: "",
    description: "",
    providerType: "OPENAI",
    modelName: "gpt-4o",
    apiUrl: "",
    authType: "API_KEY",
    apiKey: "",
    isDefault: false,
  });

  const fetchEndpoints = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get<PagedResponse<LlmEndpointSummary>>(
        `/projects/${projectId}/llm-endpoints?includeInactive=${includeInactive}&size=100`
      );
      if (response.success && response.data) {
        setEndpoints(response.data.content);
      } else {
        toast.error(response.error || "Failed to fetch LLM endpoints");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [projectId, includeInactive]);

  useEffect(() => {
    fetchEndpoints();
  }, [fetchEndpoints]);

  const resetForm = () => {
    setFormData({
      name: "",
      description: "",
      providerType: "OPENAI",
      modelName: "gpt-4o",
      apiUrl: "",
      authType: "API_KEY",
      apiKey: "",
      isDefault: false,
    });
    setShowApiKey(false);
    setFormError(null);
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setFormError("Name is required");
      return;
    }
    if (!formData.apiKey.trim()) {
      setFormError("API Key is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const response = await api.post(`/projects/${projectId}/llm-endpoints`, formData);
      if (response.success) {
        setIsCreateModalOpen(false);
        resetForm();
        toast.success("LLM endpoint created");
        fetchEndpoints();
      } else {
        setFormError(response.error || "Failed to create LLM endpoint");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async (endpointId: string) => {
    try {
      const response = await api.get<LlmEndpoint>(`/llm-endpoints/${endpointId}`);
      if (response.success && response.data) {
        setEditingEndpoint(response.data);
        setFormData({
          name: response.data.name,
          description: response.data.description || "",
          providerType: response.data.providerType,
          modelName: response.data.modelName,
          apiUrl: response.data.apiUrl || "",
          authType: response.data.authType || "API_KEY",
          apiKey: "",
          isDefault: response.data.isDefault,
        });
        setIsEditModalOpen(true);
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to load endpoint");
    }
  };

  const handleUpdate = async () => {
    if (!editingEndpoint) return;

    setSubmitting(true);
    setFormError(null);

    try {
      const updateData = {
        name: formData.name,
        description: formData.description,
        providerType: formData.providerType,
        modelName: formData.modelName,
        apiUrl: formData.apiUrl,
        authType: formData.authType,
        isDefault: formData.isDefault,
        ...(formData.apiKey && { apiKey: formData.apiKey }),
      };

      const response = await api.put(`/llm-endpoints/${editingEndpoint.id}`, updateData);
      if (response.success) {
        setIsEditModalOpen(false);
        setEditingEndpoint(null);
        resetForm();
        toast.success("LLM endpoint updated");
        fetchEndpoints();
      } else {
        setFormError(response.error || "Failed to update LLM endpoint");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (endpointId: string, endpointName: string) => {
    if (!confirm(`Are you sure you want to delete "${endpointName}"?`)) {
      return;
    }

    try {
      const response = await api.delete(`/llm-endpoints/${endpointId}`);
      if (response.success) {
        toast.success(`"${endpointName}" deleted`);
        fetchEndpoints();
      } else {
        toast.error(response.error || "Failed to delete LLM endpoint");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
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

  const renderForm = () => (
    <div className="space-y-4">
      {formError && <Alert variant="error">{formError}</Alert>}

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
          Provider
        </label>
        <select
          value={formData.providerType}
          onChange={(e) => {
            const provider = e.target.value as ProviderType;
            setFormData({
              ...formData,
              providerType: provider,
              modelName: MODEL_OPTIONS[provider][0] || "",
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
          Model
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
            placeholder="Enter model name"
          />
        )}
      </div>

      {formData.providerType === "CUSTOM" && (
        <Input
          label="API URL"
          value={formData.apiUrl}
          onChange={(e) => setFormData({ ...formData, apiUrl: e.target.value })}
          placeholder="https://api.example.com/v1/chat"
        />
      )}

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          API Key {isEditModalOpen && "(leave blank to keep existing)"}
        </label>
        <div className="relative">
          <input
            type={showApiKey ? "text" : "password"}
            value={formData.apiKey}
            onChange={(e) => setFormData({ ...formData, apiKey: e.target.value })}
            placeholder={isEditModalOpen ? "Enter new API key to update" : "Enter API key"}
            className="w-full rounded-lg border border-gray-300 px-3 py-2 pr-10 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            required={!isEditModalOpen}
          />
          <button
            type="button"
            onClick={() => setShowApiKey(!showApiKey)}
            className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600"
          >
            {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
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

      <div className="flex justify-end gap-3 pt-4">
        <Button
          variant="outline"
          onClick={() => {
            setIsCreateModalOpen(false);
            setIsEditModalOpen(false);
            setEditingEndpoint(null);
            resetForm();
          }}
        >
          Cancel
        </Button>
        <Button
          onClick={isEditModalOpen ? handleUpdate : handleCreate}
          loading={submitting}
        >
          {isEditModalOpen ? "Update" : "Create"} Endpoint
        </Button>
      </div>
    </div>
  );

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
              <h1 className="text-2xl font-bold text-gray-900">LLM Endpoints</h1>
              <p className="mt-1 text-sm text-gray-500">
                Manage LLM connections for evaluations
              </p>
            </div>
          </div>
          <Button onClick={() => setIsCreateModalOpen(true)}>
            <Plus className="h-4 w-4 mr-2" />
            Add Endpoint
          </Button>
        </div>

        {/* Filters */}
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2 text-sm text-gray-600">
            <input
              type="checkbox"
              checked={includeInactive}
              onChange={(e) => setIncludeInactive(e.target.checked)}
              className="rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            Show inactive endpoints
          </label>
        </div>

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
                    Provider
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Model
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Status
                  </th>
                  <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Actions
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
                      <Badge variant={getProviderBadgeVariant(endpoint.providerType)}>
                        {endpoint.providerType}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-600">
                      {endpoint.modelName}
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={endpoint.isActive ? "success" : "warning"}>
                        {endpoint.isActive ? "Active" : "Inactive"}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleEdit(endpoint.id)}
                          className="p-1 rounded-lg text-gray-500 hover:bg-gray-100"
                          title="Edit"
                        >
                          <Edit className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(endpoint.id, endpoint.name)}
                          className="p-1 rounded-lg text-red-500 hover:bg-red-50"
                          title="Delete"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
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
                Add an LLM endpoint to start running evaluations
              </p>
              <Button onClick={() => setIsCreateModalOpen(true)}>
                <Plus className="h-4 w-4 mr-2" />
                Add Endpoint
              </Button>
            </div>
          )}
        </div>
      </div>

      {/* Create Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => {
          setIsCreateModalOpen(false);
          resetForm();
        }}
        title="Add LLM Endpoint"
      >
        {renderForm()}
      </Modal>

      {/* Edit Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setEditingEndpoint(null);
          resetForm();
        }}
        title="Edit LLM Endpoint"
      >
        {renderForm()}
      </Modal>
    </DashboardLayout>
  );
}
