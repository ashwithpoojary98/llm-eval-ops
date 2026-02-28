"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Modal, Badge, EmptyState } from "@/components/ui";
import { api } from "@/lib/api";
import { Team, PagedResponse, CreateTeamRequest } from "@/lib/types";
import {
  Plus,
  Search,
  Users,
  MoreVertical,
  Pencil,
  Trash2,
  UserPlus,
} from "lucide-react";
import { toast } from "sonner";

export default function TeamsPage() {
  const router = useRouter();
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Modal states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [selectedTeam, setSelectedTeam] = useState<Team | null>(null);
  const [formData, setFormData] = useState({ name: "", description: "" });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Dropdown state
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);

  const fetchTeams = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get<PagedResponse<Team>>(
        `/teams?page=${page}&size=10&search=${encodeURIComponent(searchQuery)}`
      );
      if (response.success && response.data) {
        setTeams(response.data.content);
        setTotalPages(response.data.totalPages);
      } else {
        toast.error(response.error || "Failed to fetch teams");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [page, searchQuery]);

  useEffect(() => {
    fetchTeams();
  }, [fetchTeams]);

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setFormError("Team name is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const request: CreateTeamRequest = {
        name: formData.name.trim(),
        description: formData.description.trim() || undefined,
      };
      const response = await api.post<Team>("/teams", request);

      if (response.success) {
        setIsCreateModalOpen(false);
        setFormData({ name: "", description: "" });
        fetchTeams();
      } else {
        setFormError(response.error || "Failed to create team");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async () => {
    if (!selectedTeam || !formData.name.trim()) {
      setFormError("Team name is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const response = await api.put<Team>(`/teams/${selectedTeam.id}`, {
        name: formData.name.trim(),
        description: formData.description.trim() || undefined,
      });

      if (response.success) {
        setIsEditModalOpen(false);
        setSelectedTeam(null);
        setFormData({ name: "", description: "" });
        fetchTeams();
      } else {
        setFormError(response.error || "Failed to update team");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (team: Team) => {
    if (
      !confirm(
        `Are you sure you want to delete "${team.name}"? This action cannot be undone.`
      )
    ) {
      return;
    }

    try {
      const response = await api.delete(`/teams/${team.id}`);
      if (response.success) {
        toast.success(`Team "${team.name}" deleted`);
        fetchTeams();
      } else {
        toast.error(response.error || "Failed to delete team");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const openEditModal = (team: Team) => {
    setSelectedTeam(team);
    setFormData({ name: team.name, description: team.description || "" });
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
            <h1 className="text-2xl font-bold text-gray-900">Teams</h1>
            <p className="mt-1 text-sm text-gray-500">
              Manage your organization&apos;s teams
            </p>
          </div>
          <Button
            onClick={() => {
              setFormData({ name: "", description: "" });
              setFormError(null);
              setIsCreateModalOpen(true);
            }}
          >
            <Plus className="h-4 w-4 mr-2" />
            New Team
          </Button>
        </div>

        {/* Search */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search teams..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="pl-10"
            />
          </div>
        </div>

        {/* Teams List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : teams.length === 0 ? (
            <EmptyState
              icon={Users}
              title="No teams yet"
              description="Create your first team to collaborate with others on evaluation projects."
              actionLabel="Create Team"
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
                      Members
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
                  {teams.map((team) => (
                    <tr
                      key={team.id}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => router.push(`/teams/${team.id}`)}
                    >
                      <td className="px-6 py-4">
                        <div>
                          <p className="font-medium text-gray-900">
                            {team.name}
                          </p>
                          {team.description && (
                            <p className="text-sm text-gray-500 truncate max-w-md">
                              {team.description}
                            </p>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <Users className="h-4 w-4 text-gray-400" />
                          <span className="text-sm text-gray-600">
                            {team.memberCount ?? team.members?.length ?? 0} members
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {new Date(team.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="relative inline-block">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenDropdown(
                                openDropdown === team.id ? null : team.id
                              );
                            }}
                            className="p-1 rounded-lg hover:bg-gray-100"
                          >
                            <MoreVertical className="h-5 w-5 text-gray-400" />
                          </button>

                          {openDropdown === team.id && (
                            <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openEditModal(team);
                                }}
                                className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                              >
                                <Pencil className="h-4 w-4" />
                                Edit
                              </button>
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDelete(team);
                                }}
                                className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
                              >
                                <Trash2 className="h-4 w-4" />
                                Delete
                              </button>
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
                    onClick={() =>
                      setPage((p) => Math.min(totalPages - 1, p + 1))
                    }
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
        title="Create Team"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}
          <Input
            label="Team Name"
            placeholder="Enter team name"
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
              placeholder="Enter team description (optional)"
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
              Create Team
            </Button>
          </div>
        </div>
      </Modal>

      {/* Edit Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Team"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}
          <Input
            label="Team Name"
            placeholder="Enter team name"
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
              placeholder="Enter team description (optional)"
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
