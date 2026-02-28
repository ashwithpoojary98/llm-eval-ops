"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Modal, Badge, Alert, EmptyState } from "@/components/ui";
import { api } from "@/lib/api";
import { Project, PagedResponse, CreateProjectRequest } from "@/lib/types";
import {
  Plus,
  Search,
  FolderKanban,
  MoreVertical,
  Pencil,
  Trash2,
  Archive,
  RotateCcw,
} from "lucide-react";
import { toast } from "sonner";

export default function ProjectsPage() {
  const router = useRouter();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Modal states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [formData, setFormData] = useState({ name: "", description: "" });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Dropdown state
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);

  const fetchProjects = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get<PagedResponse<Project>>(
        `/projects?page=${page}&size=10&search=${encodeURIComponent(searchQuery)}`
      );
      if (response.success && response.data) {
        setProjects(response.data.content);
        setTotalPages(response.data.totalPages);
      } else {
        toast.error(response.error || "Failed to fetch projects");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [page, searchQuery]);

  useEffect(() => {
    fetchProjects();
  }, [fetchProjects]);

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setFormError("Project name is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const request: CreateProjectRequest = {
        name: formData.name.trim(),
        description: formData.description.trim() || undefined,
      };
      const response = await api.post<Project>("/projects", request);

      if (response.success) {
        setIsCreateModalOpen(false);
        setFormData({ name: "", description: "" });
        fetchProjects();
      } else {
        setFormError(response.error || "Failed to create project");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async () => {
    if (!selectedProject || !formData.name.trim()) {
      setFormError("Project name is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const response = await api.put<Project>(`/projects/${selectedProject.id}`, {
        name: formData.name.trim(),
        description: formData.description.trim() || undefined,
      });

      if (response.success) {
        setIsEditModalOpen(false);
        setSelectedProject(null);
        setFormData({ name: "", description: "" });
        fetchProjects();
      } else {
        setFormError(response.error || "Failed to update project");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleArchive = async (project: Project) => {
    if (!confirm(`Are you sure you want to archive "${project.name}"?`)) {
      return;
    }

    try {
      const response = await api.delete(`/projects/${project.id}`);
      if (response.success) {
        toast.success(`"${project.name}" archived`);
        fetchProjects();
      } else {
        toast.error(response.error || "Failed to archive project");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const handleReactivate = async (project: Project) => {
    try {
      const response = await api.post(`/projects/${project.id}/activate`);
      if (response.success) {
        toast.success(`"${project.name}" reactivated`);
        fetchProjects();
      } else {
        toast.error(response.error || "Failed to reactivate project");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const openEditModal = (project: Project) => {
    setSelectedProject(project);
    setFormData({ name: project.name, description: project.description || "" });
    setFormError(null);
    setIsEditModalOpen(true);
    setOpenDropdown(null);
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Projects</h1>
            <p className="mt-1 text-sm text-gray-500">
              Manage your evaluation projects
            </p>
          </div>
          <Button onClick={() => {
            setFormData({ name: "", description: "" });
            setFormError(null);
            setIsCreateModalOpen(true);
          }}>
            <Plus className="h-4 w-4 mr-2" />
            New Project
          </Button>
        </div>

        {/* Search */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search projects..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="pl-10"
            />
          </div>
        </div>

        {/* Projects List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : projects.length === 0 ? (
            <EmptyState
              icon={FolderKanban}
              title="No projects yet"
              description="Create your first project to start evaluating your LLM applications."
              actionLabel="Create Project"
              onAction={() => setIsCreateModalOpen(true)}
            />
          ) : (
            <>
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Name
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
                  {projects.map((project) => (
                    <tr
                      key={project.id}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => router.push(`/projects/${project.id}`)}
                    >
                      <td className="px-6 py-4">
                        <div>
                          <p className="font-medium text-gray-900">
                            {project.name}
                          </p>
                          {project.description && (
                            <p className="text-sm text-gray-500 truncate max-w-md">
                              {project.description}
                            </p>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <Badge
                          variant={
                            project.status === "ACTIVE" ? "success" : "warning"
                          }
                        >
                          {project.status}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {project.createdByName}
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {new Date(project.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="relative inline-block">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenDropdown(
                                openDropdown === project.id ? null : project.id
                              );
                            }}
                            className="p-1 rounded-lg hover:bg-gray-100"
                          >
                            <MoreVertical className="h-5 w-5 text-gray-400" />
                          </button>

                          {openDropdown === project.id && (
                            <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openEditModal(project);
                                }}
                                className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                              >
                                <Pencil className="h-4 w-4" />
                                Edit
                              </button>
                              {project.status === "ACTIVE" ? (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleArchive(project);
                                  }}
                                  className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
                                >
                                  <Archive className="h-4 w-4" />
                                  Archive
                                </button>
                              ) : (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleReactivate(project);
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
                <div className="flex items-center justify-between px-6 py-3 border-t border-gray-200">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    Previous
                  </Button>
                  <span className="text-sm text-gray-500">
                    Page {page + 1} of {totalPages}
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
        title="Create Project"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}
          <Input
            label="Project Name"
            placeholder="Enter project name"
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
              placeholder="Enter project description (optional)"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
            />
          </div>
          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => setIsCreateModalOpen(false)}
            >
              Cancel
            </Button>
            <Button onClick={handleCreate} loading={submitting}>
              Create Project
            </Button>
          </div>
        </div>
      </Modal>

      {/* Edit Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Project"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}
          <Input
            label="Project Name"
            placeholder="Enter project name"
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
              placeholder="Enter project description (optional)"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
            />
          </div>
          <div className="flex justify-end gap-3 pt-4">
            <Button variant="outline" onClick={() => setIsEditModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleEdit} loading={submitting}>
              Save Changes
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
