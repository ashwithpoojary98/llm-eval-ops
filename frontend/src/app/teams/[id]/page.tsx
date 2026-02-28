"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, Alert, Modal, Input } from "@/components/ui";
import { api } from "@/lib/api";
import { toast } from "sonner";
import { Team, TeamMemberRole, User, PagedResponse } from "@/lib/types";
import { ArrowLeft, Users, UserPlus, Trash2, Crown, UserCircle } from "lucide-react";

interface OrganizationUser {
  id: string;
  email: string;
  fullName: string;
  role: string;
}

export default function TeamDetailPage() {
  const params = useParams();
  const router = useRouter();
  const teamId = params.id as string;

  const [team, setTeam] = useState<Team | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Add member modal
  const [isAddMemberModalOpen, setIsAddMemberModalOpen] = useState(false);
  const [availableUsers, setAvailableUsers] = useState<OrganizationUser[]>([]);
  const [selectedUserId, setSelectedUserId] = useState("");
  const [selectedRole, setSelectedRole] = useState<TeamMemberRole>("MEMBER");
  const [memberFormError, setMemberFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fetchTeam = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<Team>(`/teams/${teamId}`);
      if (response.success && response.data) {
        setTeam(response.data);
      } else {
        setError(response.error || "Failed to fetch team");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [teamId]);

  const fetchAvailableUsers = async () => {
    try {
      const response = await api.get<PagedResponse<OrganizationUser>>(
        "/admin/users?size=100"
      );
      if (response.success && response.data) {
        // Filter out users already in the team
        const memberIds = team?.members?.map((m) => m.userId) || [];
        const filtered = response.data.content.filter(
          (user) => !memberIds.includes(user.id)
        );
        setAvailableUsers(filtered);
      }
    } catch (err) {
      console.error("Failed to fetch users", err);
      setAvailableUsers([]);
    }
  };

  useEffect(() => {
    fetchTeam();
  }, [fetchTeam]);

  const handleAddMember = async () => {
    if (!selectedUserId) {
      setMemberFormError("Please select a user");
      return;
    }

    setSubmitting(true);
    setMemberFormError(null);

    try {
      const response = await api.post(`/teams/${teamId}/members`, {
        userId: selectedUserId,
        role: selectedRole,
      });

      if (response.success) {
        setIsAddMemberModalOpen(false);
        setSelectedUserId("");
        setSelectedRole("MEMBER");
        toast.success("Member added successfully");
        fetchTeam();
      } else {
        setMemberFormError(response.error || "Failed to add member");
      }
    } catch (err) {
      setMemberFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemoveMember = async (userId: string, fullName: string) => {
    if (
      !confirm(`Are you sure you want to remove "${fullName}" from this team?`)
    ) {
      return;
    }

    try {
      const response = await api.delete(`/teams/${teamId}/members/${userId}`);
      if (response.success) {
        toast.success(`${fullName} removed from team`);
        fetchTeam();
      } else {
        toast.error(response.error || "Failed to remove member");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
  };

  const handleUpdateMemberRole = async (
    userId: string,
    role: TeamMemberRole
  ) => {
    try {
      const response = await api.patch(`/teams/${teamId}/members/${userId}`, {
        role,
      });
      if (response.success) {
        toast.success("Role updated");
        fetchTeam();
      } else {
        toast.error(response.error || "Failed to update member role");
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

  if (error || !team) {
    return (
      <DashboardLayout>
        <div className="space-y-4">
          <Button variant="ghost" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back
          </Button>
          <Alert variant="error">{error || "Team not found"}</Alert>
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
              <h1 className="text-2xl font-bold text-gray-900">{team.name}</h1>
              {team.description && (
                <p className="mt-1 text-sm text-gray-500">{team.description}</p>
              )}
              <p className="mt-2 text-xs text-gray-400">
                Created on {new Date(team.createdAt).toLocaleDateString()}
              </p>
            </div>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-blue-100 flex items-center justify-center">
                <Users className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {team.members?.length || 0}
                </p>
                <p className="text-sm text-gray-500">Members</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-yellow-100 flex items-center justify-center">
                <Crown className="h-5 w-5 text-yellow-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {team.members?.filter((m) => m.role === "OWNER").length || 0}
                </p>
                <p className="text-sm text-gray-500">Owners</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-green-100 flex items-center justify-center">
                <UserCircle className="h-5 w-5 text-green-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {team.members?.filter((m) => m.role === "MEMBER").length || 0}
                </p>
                <p className="text-sm text-gray-500">Regular Members</p>
              </div>
            </div>
          </div>
        </div>

        {/* Members Section */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Team Members</h2>
            <Button
              size="sm"
              onClick={() => {
                fetchAvailableUsers();
                setIsAddMemberModalOpen(true);
              }}
            >
              <UserPlus className="h-4 w-4 mr-2" />
              Add Member
            </Button>
          </div>

          {team.members && team.members.length > 0 ? (
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Member
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Email
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Role
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Joined
                  </th>
                  <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {team.members.map((member) => (
                  <tr key={member.userId} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="h-8 w-8 rounded-full bg-gray-200 flex items-center justify-center">
                          <span className="text-sm font-medium text-gray-600">
                            {member.fullName.charAt(0).toUpperCase()}
                          </span>
                        </div>
                        <span className="font-medium text-gray-900">
                          {member.fullName}
                        </span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {member.email}
                    </td>
                    <td className="px-6 py-4">
                      <select
                        value={member.role}
                        onChange={(e) =>
                          handleUpdateMemberRole(
                            member.userId,
                            e.target.value as TeamMemberRole
                          )
                        }
                        className="text-sm border border-gray-300 rounded-lg px-2 py-1 focus:outline-none focus:ring-2 focus:ring-primary-200"
                      >
                        <option value="OWNER">OWNER</option>
                        <option value="CONTRIBUTOR">CONTRIBUTOR</option>
                        <option value="MEMBER">MEMBER</option>
                      </select>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {new Date(member.joinedAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() =>
                          handleRemoveMember(member.userId, member.fullName)
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
              No members in this team yet. Add members to get started.
            </div>
          )}
        </div>
      </div>

      {/* Add Member Modal */}
      <Modal
        isOpen={isAddMemberModalOpen}
        onClose={() => setIsAddMemberModalOpen(false)}
        title="Add Team Member"
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
            {availableUsers.length === 0 && (
              <p className="mt-1 text-sm text-gray-500">
                No available users to add. All organization members are already
                in this team.
              </p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Role
            </label>
            <select
              value={selectedRole}
              onChange={(e) =>
                setSelectedRole(e.target.value as TeamMemberRole)
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="OWNER">OWNER - Team creator</option>
              <option value="CONTRIBUTOR">CONTRIBUTOR - Can contribute to team work</option>
              <option value="MEMBER">MEMBER - Regular member</option>
            </select>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => setIsAddMemberModalOpen(false)}
            >
              Cancel
            </Button>
            <Button
              onClick={handleAddMember}
              loading={submitting}
              disabled={availableUsers.length === 0}
            >
              Add Member
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
