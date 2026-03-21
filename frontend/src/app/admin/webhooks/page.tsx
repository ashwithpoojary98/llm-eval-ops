"use client";

import { useState, useEffect, useCallback } from "react";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Modal, Badge, EmptyState, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import {
  Project,
  WebhookResponse,
  WebhookDeliveryResponse,
  DeliveryStatus,
} from "@/lib/types";
import {
  Webhook,
  Plus,
  MoreVertical,
  Eye,
  Send,
  Trash2,
  Edit,
  ChevronDown,
  ChevronRight,
  Lock,
  Globe,
  FolderKanban,
} from "lucide-react";
import { toast } from "sonner";

// ── Helpers ────────────────────────────────────────────────────────────────────

function formatDate(dateString?: string) {
  if (!dateString) return "Never";
  return new Date(dateString).toLocaleString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function DeliveryStatusBadge({ status }: { status: DeliveryStatus }) {
  switch (status) {
    case "SUCCESS":
      return <Badge variant="success">Success</Badge>;
    case "FAILED":
      return <Badge variant="error">Failed</Badge>;
    case "RETRYING":
      return <Badge variant="warning">Retrying</Badge>;
    default:
      return <Badge variant="default">Pending</Badge>;
  }
}

// ── Event grouping ─────────────────────────────────────────────────────────────

const EVENT_GROUPS: Record<string, string[]> = {
  "Health Events": [],
  "Evaluation Events": [],
  "Other Events": [],
};

function groupEvents(events: string[]): Record<string, string[]> {
  const groups: Record<string, string[]> = {
    "Health Events": [],
    "Evaluation Events": [],
    "Other Events": [],
  };
  for (const ev of events) {
    if (ev.includes("HEALTH")) {
      groups["Health Events"].push(ev);
    } else if (ev.includes("EVALUATION")) {
      groups["Evaluation Events"].push(ev);
    } else {
      groups["Other Events"].push(ev);
    }
  }
  return groups;
}

// ── Webhook Form Modal ─────────────────────────────────────────────────────────

interface WebhookFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  editWebhook?: WebhookResponse | null;
  availableEvents: string[];
  projects: Project[];
}

function WebhookFormModal({
  isOpen,
  onClose,
  onSaved,
  editWebhook,
  availableEvents,
  projects,
}: WebhookFormModalProps) {
  const [name, setName] = useState("");
  const [url, setUrl] = useState("");
  const [secret, setSecret] = useState("");
  const [selectedEvents, setSelectedEvents] = useState<string[]>([]);
  const [scope, setScope] = useState<"org" | "project">("org");
  const [projectId, setProjectId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      if (editWebhook) {
        setName(editWebhook.name);
        setUrl(editWebhook.url);
        setSecret("");
        setSelectedEvents(editWebhook.events ?? []);
        setScope(editWebhook.projectId ? "project" : "org");
        setProjectId(editWebhook.projectId ?? "");
      } else {
        setName("");
        setUrl("");
        setSecret("");
        setSelectedEvents([]);
        setScope("org");
        setProjectId("");
      }
    }
  }, [isOpen, editWebhook]);

  const toggleEvent = (ev: string) => {
    setSelectedEvents((prev) =>
      prev.includes(ev) ? prev.filter((e) => e !== ev) : [...prev, ev]
    );
  };

  const handleSubmit = async () => {
    if (!name.trim()) {
      toast.error("Name is required");
      return;
    }
    if (!url.trim()) {
      toast.error("URL is required");
      return;
    }
    if (selectedEvents.length === 0) {
      toast.error("Select at least one event");
      return;
    }
    if (scope === "project" && !projectId) {
      toast.error("Select a project");
      return;
    }

    setSubmitting(true);
    try {
      const body: Record<string, unknown> = {
        name,
        url,
        events: selectedEvents,
        ...(scope === "project" ? { projectId } : {}),
        ...(secret ? { secret } : {}),
      };

      const response = editWebhook
        ? await api.put(`/webhooks/${editWebhook.id}`, body)
        : await api.post("/webhooks", body);

      if (response.success) {
        toast.success(editWebhook ? "Webhook updated" : "Webhook created");
        onSaved();
        onClose();
      } else {
        toast.error(response.error || "Failed to save webhook");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const grouped = groupEvents(availableEvents);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={editWebhook ? "Edit Webhook" : "Add Webhook"}
      size="lg"
    >
      <div className="space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
          <Input
            placeholder="My Webhook"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">URL</label>
          <Input
            placeholder="https://your-system.com/webhook"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Secret{" "}
            <span className="text-gray-400 font-normal">(optional)</span>
          </label>
          <div className="relative">
            <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              type="password"
              placeholder="Signing secret"
              value={secret}
              onChange={(e) => setSecret(e.target.value)}
              className="pl-9"
            />
          </div>
          <p className="mt-1 text-xs text-gray-500">
            Used to sign payloads with HMAC-SHA256
          </p>
        </div>

        {/* Scope */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">Scope</label>
          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => setScope("org")}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg border text-sm font-medium transition-colors ${
                scope === "org"
                  ? "border-primary-600 bg-primary-50 text-primary-700"
                  : "border-gray-200 text-gray-600 hover:bg-gray-50"
              }`}
            >
              <Globe className="h-4 w-4" />
              Organization-wide
            </button>
            <button
              type="button"
              onClick={() => setScope("project")}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg border text-sm font-medium transition-colors ${
                scope === "project"
                  ? "border-primary-600 bg-primary-50 text-primary-700"
                  : "border-gray-200 text-gray-600 hover:bg-gray-50"
              }`}
            >
              <FolderKanban className="h-4 w-4" />
              Project-specific
            </button>
          </div>

          {scope === "project" && (
            <div className="mt-3">
              <div className="relative">
                <select
                  value={projectId}
                  onChange={(e) => setProjectId(e.target.value)}
                  className="w-full appearance-none rounded-lg border border-gray-300 px-3 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500 bg-white"
                >
                  <option value="">Select a project...</option>
                  {projects.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
                <ChevronDown className="absolute right-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
              </div>
            </div>
          )}
        </div>

        {/* Events */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Events ({selectedEvents.length} selected)
          </label>
          {availableEvents.length === 0 ? (
            <p className="text-sm text-gray-500">Loading available events...</p>
          ) : (
            <div className="space-y-4 max-h-56 overflow-y-auto border border-gray-200 rounded-lg p-4">
              {Object.entries(grouped).map(([groupName, groupEvents]) => {
                if (groupEvents.length === 0) return null;
                return (
                  <div key={groupName}>
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2">
                      {groupName}
                    </p>
                    <div className="space-y-1.5">
                      {groupEvents.map((ev) => (
                        <label
                          key={ev}
                          className="flex items-center gap-2 cursor-pointer hover:bg-gray-50 rounded px-1"
                        >
                          <input
                            type="checkbox"
                            checked={selectedEvents.includes(ev)}
                            onChange={() => toggleEvent(ev)}
                            className="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                          />
                          <span className="text-sm text-gray-700 font-mono">{ev}</span>
                        </label>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 pt-2">
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} loading={submitting}>
            {editWebhook ? "Update Webhook" : "Create Webhook"}
          </Button>
        </div>
      </div>
    </Modal>
  );
}

// ── Deliveries Modal ───────────────────────────────────────────────────────────

interface DeliveriesModalProps {
  isOpen: boolean;
  onClose: () => void;
  webhook: WebhookResponse | null;
}

function DeliveriesModal({ isOpen, onClose, webhook }: DeliveriesModalProps) {
  const [deliveries, setDeliveries] = useState<WebhookDeliveryResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const fetchDeliveries = useCallback(async () => {
    if (!webhook) return;
    setLoading(true);
    try {
      const response = await api.get<WebhookDeliveryResponse[]>(
        `/webhooks/${webhook.id}/deliveries`
      );
      if (response.success && response.data) {
        setDeliveries(response.data);
      } else {
        toast.error(response.error || "Failed to fetch deliveries");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [webhook]);

  useEffect(() => {
    if (isOpen && webhook) {
      fetchDeliveries();
    }
  }, [isOpen, webhook, fetchDeliveries]);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Deliveries — ${webhook?.name ?? ""}`}
      size="xl"
    >
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      ) : deliveries.length === 0 ? (
        <EmptyState
          icon={Send}
          title="No deliveries yet"
          description="This webhook has not been triggered yet."
        />
      ) : (
        <div className="space-y-2">
          {deliveries.map((delivery) => (
            <div
              key={delivery.id}
              className="border border-gray-200 rounded-lg overflow-hidden"
            >
              <button
                className="w-full flex items-center justify-between px-4 py-3 hover:bg-gray-50 text-left"
                onClick={() =>
                  setExpandedId(expandedId === delivery.id ? null : delivery.id)
                }
              >
                <div className="flex items-center gap-3 min-w-0">
                  <DeliveryStatusBadge status={delivery.status} />
                  <span className="text-sm font-mono text-gray-700 truncate">
                    {delivery.eventType}
                  </span>
                  <span className="text-xs text-gray-400 hidden sm:block">
                    Attempt {delivery.attemptCount}/{delivery.maxAttempts}
                  </span>
                  {delivery.responseStatusCode != null && (
                    <span className="text-xs text-gray-500">
                      HTTP {delivery.responseStatusCode}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-3 flex-shrink-0 ml-2">
                  <span className="text-xs text-gray-400">
                    {formatDate(delivery.triggeredAt)}
                  </span>
                  {expandedId === delivery.id ? (
                    <ChevronDown className="h-4 w-4 text-gray-400" />
                  ) : (
                    <ChevronRight className="h-4 w-4 text-gray-400" />
                  )}
                </div>
              </button>

              {expandedId === delivery.id && (
                <div className="border-t border-gray-200 bg-gray-50 p-4 space-y-3">
                  {delivery.errorMessage && (
                    <div>
                      <p className="text-xs font-semibold text-gray-500 uppercase mb-1">
                        Error
                      </p>
                      <p className="text-sm text-red-600">{delivery.errorMessage}</p>
                    </div>
                  )}
                  <div>
                    <p className="text-xs font-semibold text-gray-500 uppercase mb-1">
                      Payload
                    </p>
                    <pre className="text-xs bg-white border border-gray-200 rounded p-3 overflow-auto max-h-48 text-gray-700">
                      {JSON.stringify(delivery.payload, null, 2)}
                    </pre>
                  </div>
                  {delivery.responseBody && (
                    <div>
                      <p className="text-xs font-semibold text-gray-500 uppercase mb-1">
                        Response Body
                      </p>
                      <pre className="text-xs bg-white border border-gray-200 rounded p-3 overflow-auto max-h-32 text-gray-700">
                        {delivery.responseBody}
                      </pre>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </Modal>
  );
}

// ── Main Page ──────────────────────────────────────────────────────────────────

export default function AdminWebhooksPage() {
  const { user } = useAuth();
  const [webhooks, setWebhooks] = useState<WebhookResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [availableEvents, setAvailableEvents] = useState<string[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);

  // Modal states
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [editWebhook, setEditWebhook] = useState<WebhookResponse | null>(null);
  const [isDeliveriesModalOpen, setIsDeliveriesModalOpen] = useState(false);
  const [selectedWebhook, setSelectedWebhook] = useState<WebhookResponse | null>(null);

  // Dropdown state
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [testingId, setTestingId] = useState<string | null>(null);

  const fetchWebhooks = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get<WebhookResponse[]>("/webhooks");
      if (response.success && response.data) {
        setWebhooks(response.data);
      } else {
        toast.error(response.error || "Failed to fetch webhooks");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchEvents = useCallback(async () => {
    try {
      const response = await api.get<string[]>("/webhooks/events");
      if (response.success && response.data) {
        setAvailableEvents(response.data);
      }
    } catch {
      // non-critical, silently fail
    }
  }, []);

  const fetchProjects = useCallback(async () => {
    try {
      const response = await api.get<Project[]>("/projects");
      if (response.success && response.data) {
        setProjects(response.data);
      }
    } catch {
      // non-critical
    }
  }, []);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      fetchWebhooks();
      fetchEvents();
      fetchProjects();
    }
  }, [fetchWebhooks, fetchEvents, fetchProjects, user?.role]);

  if (user?.role !== "ADMIN") {
    return (
      <DashboardLayout>
        <Alert variant="error">
          You do not have permission to access this page.
        </Alert>
      </DashboardLayout>
    );
  }

  // Close dropdown on outside click
  const handleBodyClick = () => {
    if (openDropdown) setOpenDropdown(null);
  };

  const handleOpenAdd = () => {
    setEditWebhook(null);
    setIsFormModalOpen(true);
  };

  const handleOpenEdit = (webhook: WebhookResponse) => {
    setEditWebhook(webhook);
    setIsFormModalOpen(true);
    setOpenDropdown(null);
  };

  const handleViewDeliveries = (webhook: WebhookResponse) => {
    setSelectedWebhook(webhook);
    setIsDeliveriesModalOpen(true);
    setOpenDropdown(null);
  };

  const handleSendTest = async (webhook: WebhookResponse) => {
    setTestingId(webhook.id);
    setOpenDropdown(null);
    try {
      const response = await api.post(`/webhooks/${webhook.id}/test`, {});
      if (response.success) {
        toast.success(`Test delivery sent for "${webhook.name}"`);
      } else {
        toast.error(response.error || "Test delivery failed");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setTestingId(null);
    }
  };

  const handleDelete = async (webhook: WebhookResponse) => {
    setOpenDropdown(null);
    if (
      !confirm(
        `Are you sure you want to delete the webhook "${webhook.name}"? This cannot be undone.`
      )
    ) {
      return;
    }
    setDeletingId(webhook.id);
    try {
      const response = await api.delete(`/webhooks/${webhook.id}`);
      if (response.success) {
        toast.success(`Webhook "${webhook.name}" deleted`);
        fetchWebhooks();
      } else {
        toast.error(response.error || "Failed to delete webhook");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <DashboardLayout>
      {/* Click-away handler */}
      <div onClick={handleBodyClick} className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Webhooks</h1>
            <p className="mt-1 text-sm text-gray-500">
              Receive real-time HTTP notifications for platform events
            </p>
          </div>
          <Button onClick={handleOpenAdd}>
            <Plus className="h-4 w-4 mr-2" />
            Add Webhook
          </Button>
        </div>

        {/* Webhooks List */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : webhooks.length === 0 ? (
            <EmptyState
              icon={Webhook}
              title="No webhooks configured"
              description="Add a webhook to receive real-time notifications for platform events."
              actionLabel="Add Webhook"
              onAction={handleOpenAdd}
            />
          ) : (
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Name / URL
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Events
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Last Triggered
                  </th>
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Failures
                  </th>
                  <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {webhooks.map((webhook) => (
                  <tr key={webhook.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 max-w-xs">
                      <p className="font-medium text-gray-900 text-sm">{webhook.name}</p>
                      <p
                        className="text-xs text-gray-500 truncate mt-0.5"
                        title={webhook.url}
                      >
                        {webhook.url}
                      </p>
                      {webhook.projectName ? (
                        <div className="mt-1 flex items-center gap-1 text-xs text-gray-400">
                          <FolderKanban className="h-3 w-3" />
                          {webhook.projectName}
                        </div>
                      ) : (
                        <div className="mt-1 flex items-center gap-1 text-xs text-gray-400">
                          <Globe className="h-3 w-3" />
                          Organization-wide
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex flex-wrap gap-1 max-w-xs">
                        {webhook.events.slice(0, 3).map((ev) => (
                          <Badge key={ev} variant="info" className="text-xs">
                            {ev}
                          </Badge>
                        ))}
                        {webhook.events.length > 3 && (
                          <Badge variant="default" className="text-xs">
                            +{webhook.events.length - 3} more
                          </Badge>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      {webhook.isActive ? (
                        <Badge variant="success">Active</Badge>
                      ) : (
                        <Badge variant="default">Inactive</Badge>
                      )}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                      {formatDate(webhook.lastTriggeredAt)}
                    </td>
                    <td className="px-6 py-4 text-sm">
                      {webhook.failureCount > 0 ? (
                        <span className="font-semibold text-red-600">
                          {webhook.failureCount}
                        </span>
                      ) : (
                        <span className="text-gray-400">0</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div
                        className="relative inline-block"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <button
                          onClick={() =>
                            setOpenDropdown(
                              openDropdown === webhook.id ? null : webhook.id
                            )
                          }
                          className="p-1 rounded-lg hover:bg-gray-100"
                          disabled={
                            deletingId === webhook.id || testingId === webhook.id
                          }
                        >
                          <MoreVertical className="h-5 w-5 text-gray-400" />
                        </button>

                        {openDropdown === webhook.id && (
                          <div className="absolute right-0 mt-1 w-52 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                            <button
                              onClick={() => handleOpenEdit(webhook)}
                              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                            >
                              <Edit className="h-4 w-4" />
                              Edit
                            </button>
                            <button
                              onClick={() => handleViewDeliveries(webhook)}
                              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                            >
                              <Eye className="h-4 w-4" />
                              View Deliveries
                            </button>
                            <button
                              onClick={() => handleSendTest(webhook)}
                              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                            >
                              <Send className="h-4 w-4" />
                              Send Test
                            </button>
                            <div className="border-t border-gray-100 my-1" />
                            <button
                              onClick={() => handleDelete(webhook)}
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
          )}
        </div>
      </div>

      {/* Form Modal */}
      <WebhookFormModal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        onSaved={fetchWebhooks}
        editWebhook={editWebhook}
        availableEvents={availableEvents}
        projects={projects}
      />

      {/* Deliveries Modal */}
      <DeliveriesModal
        isOpen={isDeliveriesModalOpen}
        onClose={() => setIsDeliveriesModalOpen(false)}
        webhook={selectedWebhook}
      />
    </DashboardLayout>
  );
}
