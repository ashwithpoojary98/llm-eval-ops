"use client";

import { useState, useEffect, useCallback } from "react";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Modal, Badge, EmptyState, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Invitation, PagedResponse, InviteUserRequest, UserRole } from "@/lib/types";
import {
  Plus,
  Search,
  Mail,
  MoreVertical,
  Send,
  Trash2,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
} from "lucide-react";
import { toast } from "sonner";

const statusConfig: Record<string, { variant: "success" | "warning" | "error" | "info" | "default"; icon: typeof CheckCircle }> = {
  PENDING: { variant: "warning", icon: Clock },
  ACCEPTED: { variant: "success", icon: CheckCircle },
  EXPIRED: { variant: "error", icon: AlertCircle },
  REVOKED: { variant: "default", icon: XCircle },
};

export default function AdminInvitationsPage() {
  const { user } = useAuth();
  const [invitations, setInvitations] = useState<Invitation[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Modal states
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [formData, setFormData] = useState({ email: "", role: "DEVELOPER" as UserRole });
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Dropdown state
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);

  const fetchInvitations = useCallback(async () => {
    setLoading(true);
    try {
      let url = `/admin/invitations?page=${page}&size=10`;
      if (searchQuery) {
        url += `&search=${encodeURIComponent(searchQuery)}`;
      }
      if (statusFilter !== "ALL") {
        url += `&status=${statusFilter}`;
      }

      const response = await api.get<PagedResponse<Invitation>>(url);
      if (response.success && response.data) {
        setInvitations(response.data.content);
        setTotalPages(response.data.totalPages);
      } else {
        toast.error(response.error || "Failed to fetch invitations");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [page, searchQuery, statusFilter]);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      fetchInvitations();
    }
  }, [fetchInvitations, user?.role]);

  // Check admin access
  if (user?.role !== "ADMIN") {
    return (
      <DashboardLayout>
        <Alert variant="error">
          You do not have permission to access this page.
        </Alert>
      </DashboardLayout>
    );
  }

  const handleInvite = async () => {
    if (!formData.email.trim()) {
      setFormError("Email is required");
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      setFormError("Please enter a valid email address");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      const request: InviteUserRequest = {
        email: formData.email.trim(),
        role: formData.role,
      };
      const response = await api.post<Invitation>("/admin/invite", request);

      if (response.success) {
        setIsInviteModalOpen(false);
        setFormData({ email: "", role: "DEVELOPER" });
        toast.success(`Invitation sent to ${request.email}`);
        fetchInvitations();
      } else {
        setFormError(response.error || "Failed to send invitation");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = async (invitation: Invitation) => {
    try {
      const response = await api.post(`/admin/invitations/${invitation.id}/resend`);
      if (response.success) {
        toast.success(`Invitation resent to ${invitation.email}`);
        fetchInvitations();
      } else {
        toast.error(response.error || "Failed to resend invitation");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const handleRevoke = async (invitation: Invitation) => {
    if (!confirm(`Are you sure you want to revoke the invitation for "${invitation.email}"?`)) {
      return;
    }

    try {
      const response = await api.delete(`/admin/invitations/${invitation.id}`);
      if (response.success) {
        toast.success(`Invitation revoked for ${invitation.email}`);
        fetchInvitations();
      } else {
        toast.error(response.error || "Failed to revoke invitation");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const isExpired = (expiresAt: string) => {
    return new Date(expiresAt) < new Date();
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Invitations</h1>
            <p className="mt-1 text-sm text-gray-500">
              Invite new users to your organization
            </p>
          </div>
          <Button
            onClick={() => {
              setFormData({ email: "", role: "DEVELOPER" });
              setFormError(null);
              setIsInviteModalOpen(true);
            }}
          >
            <Plus className="h-4 w-4 mr-2" />
            Invite User
          </Button>
        </div>

        {/* Filters */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search by email..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="pl-10"
            />
          </div>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="h-10 rounded-lg border border-gray-300 px-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
          >
            <option value="ALL">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="ACCEPTED">Accepted</option>
            <option value="EXPIRED">Expired</option>
            <option value="REVOKED">Revoked</option>
          </select>
        </div>

        {/* Invitations List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : invitations.length === 0 ? (
            <EmptyState
              icon={Mail}
              title="No invitations"
              description={
                searchQuery || statusFilter !== "ALL"
                  ? "No invitations match your filters."
                  : "Send your first invitation to grow your team."
              }
              actionLabel="Invite User"
              onAction={() => setIsInviteModalOpen(true)}
            />
          ) : (
            <>
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Email
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Role
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Invited By
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Expires
                    </th>
                    <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {invitations.map((invitation) => {
                    const status = statusConfig[invitation.status];
                    const StatusIcon = status?.icon || Clock;
                    const expired = invitation.status === "PENDING" && isExpired(invitation.expiresAt);

                    return (
                      <tr key={invitation.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <Mail className="h-4 w-4 text-gray-400" />
                            <span className="font-medium text-gray-900">
                              {invitation.email}
                            </span>
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <Badge variant="info">{invitation.role}</Badge>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <Badge variant={expired ? "error" : status?.variant || "default"}>
                              <StatusIcon className="h-3 w-3 mr-1" />
                              {expired ? "EXPIRED" : invitation.status}
                            </Badge>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-500">
                          {invitation.invitedByName}
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-500">
                          {formatDate(invitation.expiresAt)}
                        </td>
                        <td className="px-6 py-4 text-right">
                          {invitation.status === "PENDING" && (
                            <div className="relative inline-block">
                              <button
                                onClick={() =>
                                  setOpenDropdown(
                                    openDropdown === invitation.id ? null : invitation.id
                                  )
                                }
                                className="p-1 rounded-lg hover:bg-gray-100"
                              >
                                <MoreVertical className="h-5 w-5 text-gray-400" />
                              </button>

                              {openDropdown === invitation.id && (
                                <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                                  <button
                                    onClick={() => handleResend(invitation)}
                                    className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                                  >
                                    <Send className="h-4 w-4" />
                                    Resend Invitation
                                  </button>
                                  <button
                                    onClick={() => handleRevoke(invitation)}
                                    className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-gray-50"
                                  >
                                    <Trash2 className="h-4 w-4" />
                                    Revoke Invitation
                                  </button>
                                </div>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    );
                  })}
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

      {/* Invite Modal */}
      <Modal
        isOpen={isInviteModalOpen}
        onClose={() => setIsInviteModalOpen(false)}
        title="Invite User"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}

          <Input
            label="Email Address"
            type="email"
            placeholder="user@example.com"
            value={formData.email}
            onChange={(e) => setFormData({ ...formData, email: e.target.value })}
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Role
            </label>
            <select
              value={formData.role}
              onChange={(e) =>
                setFormData({ ...formData, role: e.target.value as UserRole })
              }
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
            >
              <option value="VIEWER">VIEWER - View only access</option>
              <option value="DEVELOPER">DEVELOPER - Can run evaluations</option>
              <option value="EVALUATOR">EVALUATOR - Can manage evaluations</option>
              <option value="ADMIN">ADMIN - Full access</option>
            </select>
            <p className="mt-1 text-xs text-gray-500">
              Choose the role that best fits this user&apos;s responsibilities.
            </p>
          </div>

          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => setIsInviteModalOpen(false)}
            >
              Cancel
            </Button>
            <Button onClick={handleInvite} loading={submitting}>
              <Mail className="h-4 w-4 mr-2" />
              Send Invitation
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
