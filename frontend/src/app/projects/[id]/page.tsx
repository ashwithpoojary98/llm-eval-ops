"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, Alert, Modal, Input } from "@/components/ui";
import { api } from "@/lib/api";
import { toast } from "sonner";
import { Project, Team, OrganizationUser, PagedResponse, AccessLevel } from "@/lib/types";
import {
  ArrowLeft,
  Users,
  UserPlus,
  Settings,
  Trash2,
  Database,
  FileText,
  ChevronRight,
  Play,
  Cpu,
} from "lucide-react";

export default function ProjectDetailPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const [project, setProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Teams modal
  const [isAddTeamModalOpen, setIsAddTeamModalOpen] = useState(false);
  const [availableTeams, setAvailableTeams] = useState<Team[]>([]);
  const [selectedTeamId, setSelectedTeamId] = useState("");
  const [selectedAccessLevel, setSelectedAccessLevel] = useState<AccessLevel>("READ");
  const [teamFormError, setTeamFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Members modal
  const [isAddMemberModalOpen, setIsAddMemberModalOpen] = useState(false);
  const [availableUsers, setAvailableUsers] = useState<OrganizationUser[]>([]);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedMemberAccessLevel, setSelectedMemberAccessLevel] = useState<AccessLevel>("READ");
  const [memberFormError, setMemberFormError] = useState<string | null>(null);
  const [memberSubmitting, setMemberSubmitting] = useState(false);

  const fetchProject = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<Project>(`/projects/${projectId}`);
      if (response.success && response.data) {
        setProject(response.data);
      } else {
        setError(response.error || "Failed to fetch project");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  const fetchAvailableTeams = async () => {
    try {
      const response = await api.get<PagedResponse<Team>>("/teams?size=100");
      if (response.success && response.data) {
        // Filter out teams already assigned to the project
        const assignedTeamIds = project?.teams?.map((t) => t.teamId) || [];
        const filtered = response.data.content.filter(
          (team) => !assignedTeamIds.includes(team.id)
        );
        setAvailableTeams(filtered);
      }
    } catch (err) {
      console.error("Failed to fetch teams", err);
    }
  };

  useEffect(() => {
    fetchProject();
  }, [fetchProject]);

  const handleAddTeam = async () => {
    if (!selectedTeamId) {
      setTeamFormError("Please select a team");
      return;
    }

    setSubmitting(true);
    setTeamFormError(null);

    try {
      const response = await api.post(`/projects/${projectId}/teams`, {
        teamId: selectedTeamId,
        accessLevel: selectedAccessLevel,
      });

      if (response.success) {
        setIsAddTeamModalOpen(false);
        setSelectedTeamId("");
        setSelectedAccessLevel("READ");
        toast.success("Team added to project");
        fetchProject();
      } else {
        setTeamFormError(response.error || "Failed to add team");
      }
    } catch (err) {
      setTeamFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemoveTeam = async (teamId: string, teamName: string) => {
    if (!confirm(`Are you sure you want to remove "${teamName}" from this project?`)) {
      return;
    }

    try {
      const response = await api.delete(`/projects/${projectId}/teams/${teamId}`);
      if (response.success) {
        toast.success(`"${teamName}" removed from project`);
        fetchProject();
      } else {
        toast.error(response.error || "Failed to remove team");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
  };

  const handleUpdateTeamAccess = async (teamId: string, accessLevel: AccessLevel) => {
    try {
      const response = await api.patch(`/projects/${projectId}/teams/${teamId}`, {
        accessLevel,
      });
      if (response.success) {
        toast.success("Access level updated");
        fetchProject();
      } else {
        toast.error(response.error || "Failed to update team access");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
  };

  const fetchAvailableUsers = async () => {
    try {
      const response = await api.get<PagedResponse<OrganizationUser>>("/users?size=100");
      if (response.success && response.data) {
        const assignedUserIds = project?.members?.map((m) => m.userId) || [];
        const filtered = response.data.content.filter(
          (u) => u.isActive && !assignedUserIds.includes(u.id)
        );
        setAvailableUsers(filtered);
      }
    } catch (err) {
      console.error("Failed to fetch users", err);
    }
  };

  const handleAddMember = async () => {
    if (!selectedUserId) {
      setMemberFormError("Please select a user");
      return;
    }

    setMemberSubmitting(true);
    setMemberFormError(null);

    try {
      const response = await api.post(`/projects/${projectId}/members`, {
        userId: selectedUserId,
        accessLevel: selectedMemberAccessLevel,
      });

      if (response.success) {
        setIsAddMemberModalOpen(false);
        setSelectedUserId("");
        setSelectedMemberAccessLevel("READ");
        toast.success("Member added to project");
        fetchProject();
      } else {
        setMemberFormError(response.error || "Failed to add member");
      }
    } catch (err) {
      setMemberFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setMemberSubmitting(false);
    }
  };

  const handleRemoveMember = async (userId: string, fullName: string) => {
    if (!confirm(`Are you sure you want to remove "${fullName}" from this project?`)) {
      return;
    }

    try {
      const response = await api.delete(`/projects/${projectId}/members/${userId}`);
      if (response.success) {
        toast.success(`"${fullName}" removed from project`);
        fetchProject();
      } else {
        toast.error(response.error || "Failed to remove member");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
  };

  const handleUpdateMemberAccess = async (userId: string, accessLevel: AccessLevel) => {
    try {
      const response = await api.patch(`/projects/${projectId}/members/${userId}`, {
        accessLevel,
      });
      if (response.success) {
        toast.success("Access level updated");
        fetchProject();
      } else {
        toast.error(response.error || "Failed to update member access");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
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

  if (error || !project) {
    return (
      <DashboardLayout>
        <div className="space-y-4">
          <Button variant="ghost" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back
          </Button>
          <Alert variant="error">{error || "Project not found"}</Alert>
        </div>
      </DashboardLayout>
    );
  }

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
                <h1 className="text-2xl font-bold text-gray-900">
                  {project.name}
                </h1>
                <Badge
                  variant={project.status === "ACTIVE" ? "success" : "warning"}
                >
                  {project.status}
                </Badge>
              </div>
              {project.description && (
                <p className="mt-1 text-sm text-gray-500">
                  {project.description}
                </p>
              )}
              <p className="mt-2 text-xs text-gray-400">
                Created by {project.createdByName} on{" "}
                {new Date(project.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-blue-100 flex items-center justify-center">
                <Users className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {project.teams?.length || 0}
                </p>
                <p className="text-sm text-gray-500">Teams</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-green-100 flex items-center justify-center">
                <UserPlus className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {project.members?.length || 0}
                </p>
                <p className="text-sm text-gray-500">Members</p>
              </div>
            </div>
          </div>
          <Link
            href={`/projects/${projectId}/datasets`}
            className="bg-white rounded-xl border border-gray-200 p-4 hover:border-purple-300 hover:shadow-sm transition-all"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-purple-100 flex items-center justify-center">
                  <Database className="h-5 w-5 text-purple-600" />
                </div>
                <div>
                  <p className="text-lg font-semibold text-gray-900">Datasets</p>
                  <p className="text-sm text-gray-500">Manage datasets</p>
                </div>
              </div>
              <ChevronRight className="h-5 w-5 text-gray-400" />
            </div>
          </Link>
          <Link
            href={`/projects/${projectId}/testcases`}
            className="bg-white rounded-xl border border-gray-200 p-4 hover:border-blue-300 hover:shadow-sm transition-all"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-orange-100 flex items-center justify-center">
                  <FileText className="h-5 w-5 text-orange-600" />
                </div>
                <div>
                  <p className="text-lg font-semibold text-gray-900">Test Cases</p>
                  <p className="text-sm text-gray-500">Manage test cases</p>
                </div>
              </div>
              <ChevronRight className="h-5 w-5 text-gray-400" />
            </div>
          </Link>
          <Link
            href={`/projects/${projectId}/evaluations`}
            className="bg-white rounded-xl border border-gray-200 p-4 hover:border-green-300 hover:shadow-sm transition-all"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-green-100 flex items-center justify-center">
                  <Play className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-lg font-semibold text-gray-900">Evaluations</p>
                  <p className="text-sm text-gray-500">Run & view evaluations</p>
                </div>
              </div>
              <ChevronRight className="h-5 w-5 text-gray-400" />
            </div>
          </Link>
          <Link
            href={`/projects/${projectId}/llm-endpoints`}
            className="bg-white rounded-xl border border-gray-200 p-4 hover:border-indigo-300 hover:shadow-sm transition-all"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-indigo-100 flex items-center justify-center">
                  <Cpu className="h-5 w-5 text-indigo-600" />
                </div>
                <div>
                  <p className="text-lg font-semibold text-gray-900">LLM Endpoints</p>
                  <p className="text-sm text-gray-500">Manage LLM connections</p>
                </div>
              </div>
              <ChevronRight className="h-5 w-5 text-gray-400" />
            </div>
          </Link>
        </div>

        {/* Teams Section */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Teams</h2>
            <Button
              size="sm"
              onClick={() => {
                fetchAvailableTeams();
                setIsAddTeamModalOpen(true);
              }}
            >
              <UserPlus className="h-4 w-4 mr-2" />
              Add Team
            </Button>
          </div>

          {project.teams && project.teams.length > 0 ? (
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Team Name
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Access Level
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Assigned At
                  </th>
                  <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {project.teams.map((team) => (
                  <tr key={team.teamId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 font-medium text-gray-900">
                      {team.teamName}
                    </td>
                    <td className="px-6 py-4">
                      <select
                        value={team.accessLevel}
                        onChange={(e) =>
                          handleUpdateTeamAccess(
                            team.teamId,
                            e.target.value as AccessLevel
                          )
                        }
                        className="text-sm border border-gray-300 rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-primary-200"
                      >
                        <option value="READ">READ</option>
                        <option value="WRITE">WRITE</option>
                        <option value="ADMIN">ADMIN</option>
                      </select>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {new Date(team.assignedAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() =>
                          handleRemoveTeam(team.teamId, team.teamName)
                        }
                        className="p-1 rounded-lg text-red-500 hover:bg-red-50"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="px-6 py-8 text-center text-gray-500">
              No teams assigned to this project yet.
            </div>
          )}
        </div>

        {/* Members Section */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">
              Direct Members
            </h2>
            <Button
              size="sm"
              variant="outline"
              onClick={() => {
                fetchAvailableUsers();
                setSelectedUserId("");
                setSelectedMemberAccessLevel("READ");
                setMemberFormError(null);
                setIsAddMemberModalOpen(true);
              }}
            >
              <UserPlus className="h-4 w-4 mr-2" />
              Add Member
            </Button>
          </div>

          {project.members && project.members.length > 0 ? (
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Name
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Email
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Access Level
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Assigned At
                  </th>
                  <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {project.members.map((member) => (
                  <tr key={member.userId} className="hover:bg-gray-50">
                    <td className="px-6 py-4 font-medium text-gray-900">
                      {member.fullName}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {member.email}
                    </td>
                    <td className="px-6 py-4">
                      <select
                        value={member.accessLevel}
                        onChange={(e) =>
                          handleUpdateMemberAccess(member.userId, e.target.value as AccessLevel)
                        }
                        className="text-sm border border-gray-300 rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-primary-200"
                      >
                        <option value="READ">READ</option>
                        <option value="WRITE">WRITE</option>
                        <option value="ADMIN">ADMIN</option>
                      </select>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {new Date(member.assignedAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleRemoveMember(member.userId, member.fullName)}
                        className="p-1 rounded-lg text-red-500 hover:bg-red-50"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="px-6 py-8 text-center text-gray-500">
              No direct members assigned to this project yet.
            </div>
          )}
        </div>
      </div>

      {/* Add Member Modal */}
      <Modal
        isOpen={isAddMemberModalOpen}
        onClose={() => setIsAddMemberModalOpen(false)}
        title="Add Member to Project"
      >
        <div className="space-y-4">
          {memberFormError && <Alert variant="error">{memberFormError}</Alert>}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Select User
            </label>
            <select
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="">Choose a user...</option>
              {availableUsers.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.fullName} ({user.email})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Access Level
            </label>
            <select
              value={selectedMemberAccessLevel}
              onChange={(e) => setSelectedMemberAccessLevel(e.target.value as AccessLevel)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="READ">READ - View only</option>
              <option value="WRITE">WRITE - Can edit</option>
              <option value="ADMIN">ADMIN - Full access</option>
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button variant="outline" onClick={() => setIsAddMemberModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleAddMember} loading={memberSubmitting}>
              Add Member
            </Button>
          </div>
        </div>
      </Modal>

      {/* Add Team Modal */}
      <Modal
        isOpen={isAddTeamModalOpen}
        onClose={() => setIsAddTeamModalOpen(false)}
        title="Add Team to Project"
      >
        <div className="space-y-4">
          {teamFormError && <Alert variant="error">{teamFormError}</Alert>}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Select Team
            </label>
            <select
              value={selectedTeamId}
              onChange={(e) => setSelectedTeamId(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="">Choose a team...</option>
              {availableTeams.map((team) => (
                <option key={team.id} value={team.id}>
                  {team.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Access Level
            </label>
            <select
              value={selectedAccessLevel}
              onChange={(e) =>
                setSelectedAccessLevel(e.target.value as AccessLevel)
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="READ">READ - View only</option>
              <option value="WRITE">WRITE - Can edit</option>
              <option value="ADMIN">ADMIN - Full access</option>
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => setIsAddTeamModalOpen(false)}
            >
              Cancel
            </Button>
            <Button onClick={handleAddTeam} loading={submitting}>
              Add Team
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
