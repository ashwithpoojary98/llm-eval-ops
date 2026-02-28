"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Modal, Badge, EmptyState, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { Dataset, Project, PagedResponse, CreateDatasetRequest } from "@/lib/types";
import {
  Plus,
  Search,
  Database,
  MoreVertical,
  Pencil,
  Trash2,
  RotateCcw,
  ArrowLeft,
  FileText,
} from "lucide-react";
import { toast } from "sonner";

export default function ProjectDatasetsPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const [project, setProject] = useState<Project | null>(null);
  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Modal states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedDataset, setSelectedDataset] = useState<Dataset | null>(null);
  const [formData, setFormData] = useState({ name: "", description: "" });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Dropdown state
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);

  const fetchProject = useCallback(async () => {
    try {
      const response = await api.get<Project>(`/projects/${projectId}`);
      if (response.success && response.data) {
        setProject(response.data);
      }
    } catch (err) {
      console.error("Failed to fetch project", err);
    }
  }, [projectId]);

  const fetchDatasets = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get<PagedResponse<Dataset>>(
        `/projects/${projectId}/datasets?page=${page}&size=10&search=${encodeURIComponent(searchQuery)}`
      );
      if (response.success && response.data) {
        setDatasets(response.data.content);
        setTotalPages(response.data.totalPages);
      } else {
        toast.error(response.error || "Failed to fetch datasets");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [projectId, page, searchQuery]);

  useEffect(() => {
    fetchProject();
    fetchDatasets();
  }, [fetchProject, fetchDatasets]);

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setFormError("Dataset name is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const request: CreateDatasetRequest = {
        name: formData.name.trim(),
        description: formData.description.trim() || undefined,
      };
      const response = await api.post<Dataset>(`/projects/${projectId}/datasets`, request);

      if (response.success) {
        setIsCreateModalOpen(false);
        setFormData({ name: "", description: "" });
        fetchDatasets();
      } else {
        setFormError(response.error || "Failed to create dataset");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async () => {
    if (!selectedDataset || !formData.name.trim()) {
      setFormError("Dataset name is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const response = await api.put<Dataset>(`/datasets/${selectedDataset.id}`, {
        name: formData.name.trim(),
        description: formData.description.trim() || undefined,
      });

      if (response.success) {
        setIsEditModalOpen(false);
        setSelectedDataset(null);
        setFormData({ name: "", description: "" });
        fetchDatasets();
      } else {
        setFormError(response.error || "Failed to update dataset");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (dataset: Dataset) => {
    if (!confirm(`Are you sure you want to delete "${dataset.name}"?`)) {
      return;
    }

    try {
      const response = await api.delete(`/datasets/${dataset.id}`);
      if (response.success) {
        toast.success(`"${dataset.name}" deleted`);
        fetchDatasets();
      } else {
        toast.error(response.error || "Failed to delete dataset");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const handleReactivate = async (dataset: Dataset) => {
    try {
      const response = await api.post(`/datasets/${dataset.id}/reactivate`);
      if (response.success) {
        toast.success(`"${dataset.name}" reactivated`);
        fetchDatasets();
      } else {
        toast.error(response.error || "Failed to reactivate dataset");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const openEditModal = (dataset: Dataset) => {
    setSelectedDataset(dataset);
    setFormData({ name: dataset.name, description: dataset.description || "" });
    setFormError(null);
    setIsEditModalOpen(true);
    setOpenDropdown(null);
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <Button variant="ghost" size="sm" onClick={() => router.push(`/projects/${projectId}`)}>
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <div className="hidden sm:flex items-center gap-2 text-sm text-gray-500 mb-1">
                <Link href="/projects" className="hover:text-primary-600">Projects</Link>
                <span>/</span>
                <Link href={`/projects/${projectId}`} className="hover:text-primary-600 truncate max-w-[150px]">
                  {project?.name || "..."}
                </Link>
                <span>/</span>
                <span>Datasets</span>
              </div>
              <h1 className="text-xl sm:text-2xl font-bold text-gray-900">Datasets</h1>
            </div>
          </div>
          <Button
            onClick={() => {
              setFormData({ name: "", description: "" });
              setFormError(null);
              setIsCreateModalOpen(true);
            }}
            className="w-full sm:w-auto"
          >
            <Plus className="h-4 w-4 mr-2" />
            New Dataset
          </Button>
        </div>

        {/* Search */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 sm:max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search datasets..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="pl-10"
            />
          </div>
        </div>

        {/* Datasets List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : datasets.length === 0 ? (
            <EmptyState
              icon={Database}
              title="No datasets yet"
              description="Create your first dataset to organize test cases for evaluation."
              actionLabel="Create Dataset"
              onAction={() => setIsCreateModalOpen(true)}
            />
          ) : (
            <>
              {/* Mobile Card View */}
              <div className="block md:hidden divide-y divide-gray-200">
                {datasets.map((dataset) => (
                  <div
                    key={dataset.id}
                    className="p-4 hover:bg-gray-50 cursor-pointer"
                    onClick={() => router.push(`/projects/${projectId}/datasets/${dataset.id}`)}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-start gap-3 flex-1 min-w-0">
                        <div className="h-9 w-9 rounded-lg bg-purple-100 flex items-center justify-center flex-shrink-0">
                          <Database className="h-5 w-5 text-purple-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <p className="font-medium text-gray-900 truncate">{dataset.name}</p>
                            <Badge variant={dataset.isActive ? "success" : "warning"} className="flex-shrink-0">
                              {dataset.isActive ? "Active" : "Inactive"}
                            </Badge>
                          </div>
                          {dataset.description && (
                            <p className="text-sm text-gray-500 truncate mb-2">
                              {dataset.description}
                            </p>
                          )}
                          <div className="flex items-center gap-2 text-xs text-gray-400">
                            <FileText className="h-3 w-3" />
                            <span>{dataset.testCaseCount} test cases</span>
                            <span>·</span>
                            <span>{dataset.createdByName}</span>
                          </div>
                        </div>
                      </div>
                      <div className="relative flex-shrink-0">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setOpenDropdown(openDropdown === dataset.id ? null : dataset.id);
                          }}
                          className="p-1 rounded-lg hover:bg-gray-100"
                        >
                          <MoreVertical className="h-5 w-5 text-gray-400" />
                        </button>

                        {openDropdown === dataset.id && (
                          <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                openEditModal(dataset);
                              }}
                              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                            >
                              <Pencil className="h-4 w-4" />
                              Edit
                            </button>
                            {dataset.isActive ? (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDelete(dataset);
                                }}
                                className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
                              >
                                <Trash2 className="h-4 w-4" />
                                Delete
                              </button>
                            ) : (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleReactivate(dataset);
                                }}
                                className="w-full flex items-center gap-2 px-4 py-2 text-sm text-green-600 hover:bg-gray-50"
                              >
                                <RotateCcw className="h-4 w-4" />
                                Reactivate
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Desktop Table View */}
              <table className="hidden md:table w-full">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Name
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Test Cases
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Created By
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Created At
                    </th>
                    <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {datasets.map((dataset) => (
                    <tr
                      key={dataset.id}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => router.push(`/projects/${projectId}/datasets/${dataset.id}`)}
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="h-9 w-9 rounded-lg bg-purple-100 flex items-center justify-center flex-shrink-0">
                            <Database className="h-5 w-5 text-purple-600" />
                          </div>
                          <div className="min-w-0">
                            <p className="font-medium text-gray-900 truncate">{dataset.name}</p>
                            {dataset.description && (
                              <p className="text-sm text-gray-500 truncate max-w-xs">
                                {dataset.description}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <FileText className="h-4 w-4 text-gray-400" />
                          <span className="text-sm text-gray-600">
                            {dataset.testCaseCount} test cases
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant={dataset.isActive ? "success" : "warning"}>
                          {dataset.isActive ? "Active" : "Inactive"}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {dataset.createdByName}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {new Date(dataset.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="relative inline-block">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenDropdown(openDropdown === dataset.id ? null : dataset.id);
                            }}
                            className="p-1 rounded-lg hover:bg-gray-100"
                          >
                            <MoreVertical className="h-5 w-5 text-gray-400" />
                          </button>

                          {openDropdown === dataset.id && (
                            <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openEditModal(dataset);
                                }}
                                className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                              >
                                <Pencil className="h-4 w-4" />
                                Edit
                              </button>
                              {dataset.isActive ? (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleDelete(dataset);
                                  }}
                                  className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
                                >
                                  <Trash2 className="h-4 w-4" />
                                  Delete
                                </button>
                              ) : (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleReactivate(dataset);
                                  }}
                                  className="w-full flex items-center gap-2 px-4 py-2 text-sm text-green-600 hover:bg-gray-50"
                                >
                                  <RotateCcw className="h-4 w-4" />
                                  Reactivate
                                </button>
                              )}
                            </div>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="flex items-center justify-between px-4 sm:px-6 py-3 border-t border-gray-200">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    Previous
                  </Button>
                  <span className="text-sm text-gray-500">
                    {page + 1} / {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                  >
                    Next
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Create Modal */}
      <Modal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        title="Create Dataset"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}
          <Input
            label="Dataset Name"
            placeholder="Enter dataset name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              rows={3}
              placeholder="Enter dataset description (optional)"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>
          <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 pt-4">
            <Button variant="outline" onClick={() => setIsCreateModalOpen(false)} className="w-full sm:w-auto">
              Cancel
            </Button>
            <Button onClick={handleCreate} loading={submitting} className="w-full sm:w-auto">
              Create Dataset
            </Button>
          </div>
        </div>
      </Modal>

      {/* Edit Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Dataset"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}
          <Input
            label="Dataset Name"
            placeholder="Enter dataset name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          />
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              rows={3}
              placeholder="Enter dataset description (optional)"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>
          <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 pt-4">
            <Button variant="outline" onClick={() => setIsEditModalOpen(false)} className="w-full sm:w-auto">
              Cancel
            </Button>
            <Button onClick={handleEdit} loading={submitting} className="w-full sm:w-auto">
              Save Changes
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
