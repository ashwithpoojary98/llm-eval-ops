"use client";

import { useState, useEffect, useCallback } from "react";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Modal, Badge, EmptyState, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { OrganizationUser, PagedResponse, UserRole } from "@/lib/types";
import {
  Search,
  Users,
  MoreVertical,
  Shield,
  UserX,
  UserCheck,
  Mail,
  Calendar,
  Clock,
} from "lucide-react";
import { toast } from "sonner";
import Link from "next/link";

const roleColors: Record<UserRole, "error" | "warning" | "info" | "default"> = {
  ADMIN: "error",
  EVALUATOR: "warning",
  DEVELOPER: "info",
  VIEWER: "default",
};

export default function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<OrganizationUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Modal states
  const [isRoleModalOpen, setIsRoleModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<OrganizationUser | null>(null);
  const [selectedRole, setSelectedRole] = useState<UserRole>("DEVELOPER");
  const [submitting, setSubmitting] = useState(false);

  // Dropdown state
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      let url = `/admin/users?page=${page}&size=10`;
      if (searchQuery) {
        url += `&search=${encodeURIComponent(searchQuery)}`;
      }

      const response = await api.get<PagedResponse<OrganizationUser>>(url);
      if (response.success && response.data) {
        setUsers(response.data.content);
        setTotalPages(response.data.totalPages);
        setTotalElements(response.data.totalElements);
      } else {
        toast.error(response.error || "Failed to fetch users");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [page, searchQuery]);

  useEffect(() => {
    if (currentUser?.role === "ADMIN") {
      fetchUsers();
    }
  }, [fetchUsers, currentUser?.role]);

  // Check admin access
  if (currentUser?.role !== "ADMIN") {
    return (
      <DashboardLayout>
        <Alert variant="error">
          You do not have permission to access this page.
        </Alert>
      </DashboardLayout>
    );
  }

  const handleUpdateRole = async () => {
    if (!selectedUser) return;

    setSubmitting(true);
    try {
      const response = await api.patch(`/admin/users/${selectedUser.id}/role`, {
        role: selectedRole,
      });

      if (response.success) {
        setIsRoleModalOpen(false);
        setSelectedUser(null);
        toast.success(`Role updated for ${selectedUser.fullName}`);
        fetchUsers();
      } else {
        toast.error(response.error || "Failed to update role");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeactivate = async (user: OrganizationUser) => {
    if (user.id === currentUser?.userId) {
      toast.error("You cannot deactivate your own account");
      return;
    }

    if (!confirm(`Are you sure you want to deactivate "${user.fullName}"? They will no longer be able to access the platform.`)) {
      return;
    }

    try {
      const response = await api.delete(`/admin/users/${user.id}`);
      if (response.success) {
        toast.success(`${user.fullName} has been deactivated`);
        fetchUsers();
      } else {
        toast.error(response.error || "Failed to deactivate user");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const handleReactivate = async (user: OrganizationUser) => {
    try {
      const response = await api.post(`/admin/users/${user.id}/reactivate`);
      if (response.success) {
        toast.success(`${user.fullName} has been reactivated`);
        fetchUsers();
      } else {
        toast.error(response.error || "Failed to reactivate user");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const openRoleModal = (user: OrganizationUser) => {
    setSelectedUser(user);
    setSelectedRole(user.role);
    setIsRoleModalOpen(true);
    setOpenDropdown(null);
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return "Never";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Users</h1>
            <p className="mt-1 text-sm text-gray-500">
              Manage organization users ({totalElements} total)
            </p>
          </div>
          <Link href="/admin/invitations">
            <Button>
              <Mail className="h-4 w-4 mr-2" />
              Invite User
            </Button>
          </Link>
        </div>

        {/* Search */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search by name or email..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="pl-10"
            />
          </div>
        </div>

        {/* Users List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : users.length === 0 ? (
            <EmptyState
              icon={Users}
              title="No users found"
              description={
                searchQuery
                  ? "No users match your search."
                  : "No users in your organization yet."
              }
            />
          ) : (
            <>
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      User
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Role
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Last Login
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Joined
                    </th>
                    <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {users.map((user) => (
                    <tr key={user.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="h-10 w-10 rounded-full bg-gray-200 flex items-center justify-center">
                            <span className="text-sm font-medium text-gray-600">
                              {user.fullName.charAt(0).toUpperCase()}
                            </span>
                          </div>
                          <div>
                            <p className="font-medium text-gray-900">
                              {user.fullName}
                              {user.id === currentUser?.userId && (
                                <span className="ml-2 text-xs text-gray-400">(You)</span>
                              )}
                            </p>
                            <p className="text-sm text-gray-500">{user.email}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant={roleColors[user.role]}>{user.role}</Badge>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          {user.isActive ? (
                            <Badge variant="success">Active</Badge>
                          ) : (
                            <Badge variant="error">Inactive</Badge>
                          )}
                          {user.emailVerified && (
                            <span className="text-green-500" title="Email verified">
                              <Mail className="h-4 w-4" />
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1 text-sm text-gray-500">
                          <Clock className="h-4 w-4" />
                          {formatDate(user.lastLoginAt)}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-1 text-sm text-gray-500">
                          <Calendar className="h-4 w-4" />
                          {formatDate(user.createdAt)}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-right">
                        {user.id !== currentUser?.userId && (
                          <div className="relative inline-block">
                            <button
                              onClick={() =>
                                setOpenDropdown(
                                  openDropdown === user.id ? null : user.id
                                )
                              }
                              className="p-1 rounded-lg hover:bg-gray-100"
                            >
                              <MoreVertical className="h-5 w-5 text-gray-400" />
                            </button>

                            {openDropdown === user.id && (
                              <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                                <button
                                  onClick={() => openRoleModal(user)}
                                  className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                >
                                  <Shield className="h-4 w-4" />
                                  Change Role
                                </button>
                                {user.isActive ? (
                                  <button
                                    onClick={() => handleDeactivate(user)}
                                    className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
                                  >
                                    <UserX className="h-4 w-4" />
                                    Deactivate
                                  </button>
                                ) : (
                                  <button
                                    onClick={() => handleReactivate(user)}
                                    className="w-full flex items-center gap-2 px-4 py-2 text-sm text-green-600 hover:bg-gray-50"
                                  >
                                    <UserCheck className="h-4 w-4" />
                                    Reactivate
                                  </button>
                                )}
                              </div>
                            )}
                          </div>
                        )}
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

      {/* Change Role Modal */}
      <Modal
        isOpen={isRoleModalOpen}
        onClose={() => setIsRoleModalOpen(false)}
        title="Change User Role"
      >
        <div className="space-y-4">
          {selectedUser && (
            <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
              <div className="h-10 w-10 rounded-full bg-gray-200 flex items-center justify-center">
                <span className="text-sm font-medium text-gray-600">
                  {selectedUser.fullName.charAt(0).toUpperCase()}
                </span>
              </div>
              <div>
                <p className="font-medium text-gray-900">{selectedUser.fullName}</p>
                <p className="text-sm text-gray-500">{selectedUser.email}</p>
              </div>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              New Role
            </label>
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value as UserRole)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="VIEWER">VIEWER - View only access</option>
              <option value="DEVELOPER">DEVELOPER - Can run evaluations</option>
              <option value="EVALUATOR">EVALUATOR - Can manage evaluations</option>
              <option value="ADMIN">ADMIN - Full access</option>
            </select>
          </div>

          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
            <p className="text-sm text-yellow-800">
              <strong>Note:</strong> Changing a user&apos;s role will immediately affect
              their permissions across the platform.
            </p>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button variant="outline" onClick={() => setIsRoleModalOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleUpdateRole} loading={submitting}>
              Update Role
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
