"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Modal, Badge, EmptyState, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { TestCase, Project, PagedResponse, CreateTestCaseRequest } from "@/lib/types";
import {
  Plus,
  Search,
  FileText,
  MoreVertical,
  Pencil,
  Trash2,
  RotateCcw,
  ArrowLeft,
  Eye,
  X,
} from "lucide-react";
import { toast } from "sonner";

export default function ProjectTestCasesPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const [project, setProject] = useState<Project | null>(null);
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Modal states
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [selectedTestCase, setSelectedTestCase] = useState<TestCase | null>(null);
  const [formData, setFormData] = useState({
    question: "",
    llmOutput: "",
    retrievedContext: [] as string[],
    groundTruth: "",
  });
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

  const fetchTestCases = useCallback(async () => {
    setLoading(true);
    try {
      const response = await api.get<PagedResponse<TestCase>>(
        `/projects/${projectId}/testcases?page=${page}&size=10&search=${encodeURIComponent(searchQuery)}`
      );
      if (response.success && response.data) {
        setTestCases(response.data.content);
        setTotalPages(response.data.totalPages);
      } else {
        toast.error(response.error || "Failed to fetch test cases");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [projectId, page, searchQuery]);

  useEffect(() => {
    fetchProject();
    fetchTestCases();
  }, [fetchProject, fetchTestCases]);

  const resetForm = () => {
    setFormData({
      question: "",
      llmOutput: "",
      retrievedContext: [],
      groundTruth: "",
    });
    setFormError(null);
  };

  const addContextItem = () => {
    setFormData({
      ...formData,
      retrievedContext: [...formData.retrievedContext, ""],
    });
  };

  const updateContextItem = (index: number, value: string) => {
    const newContext = [...formData.retrievedContext];
    newContext[index] = value;
    setFormData({ ...formData, retrievedContext: newContext });
  };

  const removeContextItem = (index: number) => {
    const newContext = formData.retrievedContext.filter((_, i) => i !== index);
    setFormData({ ...formData, retrievedContext: newContext });
  };

  const handleCreate = async () => {
    if (!formData.question.trim()) {
      setFormError("Question is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      // Filter out empty context items
      const contextArray = formData.retrievedContext
        .map(s => s.trim())
        .filter(s => s);

      const request: CreateTestCaseRequest = {
        question: formData.question.trim(),
        llmOutput: formData.llmOutput.trim() || undefined,
        retrievedContext: contextArray.length > 0 ? contextArray : undefined,
        groundTruth: formData.groundTruth.trim() || undefined,
      };
      const response = await api.post<TestCase>(`/projects/${projectId}/testcases`, request);

      if (response.success) {
        setIsCreateModalOpen(false);
        resetForm();
        toast.success("Test case created");
        fetchTestCases();
      } else {
        setFormError(response.error || "Failed to create test case");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEdit = async () => {
    if (!selectedTestCase || !formData.question.trim()) {
      setFormError("Question is required");
      return;
    }

    setSubmitting(true);
    setFormError(null);

    try {
      // Filter out empty context items
      const contextArray = formData.retrievedContext
        .map(s => s.trim())
        .filter(s => s);

      const response = await api.put<TestCase>(`/testcases/${selectedTestCase.id}`, {
        question: formData.question.trim(),
        llmOutput: formData.llmOutput.trim() || undefined,
        retrievedContext: contextArray.length > 0 ? contextArray : undefined,
        groundTruth: formData.groundTruth.trim() || undefined,
      });

      if (response.success) {
        setIsEditModalOpen(false);
        setSelectedTestCase(null);
        resetForm();
        toast.success("Test case updated");
        fetchTestCases();
      } else {
        setFormError(response.error || "Failed to update test case");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (testCase: TestCase) => {
    const preview = testCase.question.length > 50 ? testCase.question.substring(0, 50) + "..." : testCase.question;
    if (!confirm(`Are you sure you want to delete this test case?\n\n"${preview}"`)) {
      return;
    }

    try {
      const response = await api.delete(`/testcases/${testCase.id}`);
      if (response.success) {
        toast.success("Test case deleted");
        fetchTestCases();
      } else {
        toast.error(response.error || "Failed to delete test case");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const handleReactivate = async (testCase: TestCase) => {
    try {
      const response = await api.post(`/testcases/${testCase.id}/reactivate`);
      if (response.success) {
        toast.success("Test case reactivated");
        fetchTestCases();
      } else {
        toast.error(response.error || "Failed to reactivate test case");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
    setOpenDropdown(null);
  };

  const openEditModal = async (testCase: TestCase) => {
    setOpenDropdown(null);
    try {
      // Fetch full test case details
      const response = await api.get<TestCase>(`/testcases/${testCase.id}`);
      if (response.success && response.data) {
        const fullTestCase = response.data;
        setSelectedTestCase(fullTestCase);
        setFormData({
          question: fullTestCase.question,
          llmOutput: fullTestCase.llmOutput || "",
          retrievedContext: fullTestCase.retrievedContext || [],
          groundTruth: fullTestCase.groundTruth || "",
        });
        setFormError(null);
        setIsEditModalOpen(true);
      } else {
        toast.error(response.error || "Failed to fetch test case details");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to fetch test case details");
    }
  };

  const openViewModal = async (testCase: TestCase) => {
    setOpenDropdown(null);
    try {
      // Fetch full test case details
      const response = await api.get<TestCase>(`/testcases/${testCase.id}`);
      if (response.success && response.data) {
        setSelectedTestCase(response.data);
        setIsViewModalOpen(true);
      } else {
        toast.error(response.error || "Failed to fetch test case details");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed to fetch test case details");
    }
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
                <span>Test Cases</span>
              </div>
              <h1 className="text-xl sm:text-2xl font-bold text-gray-900">Test Cases</h1>
            </div>
          </div>
          <Button
            onClick={() => {
              resetForm();
              setIsCreateModalOpen(true);
            }}
            className="w-full sm:w-auto"
          >
            <Plus className="h-4 w-4 mr-2" />
            New Test Case
          </Button>
        </div>

        {/* Search */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 sm:max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search test cases..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                setPage(0);
              }}
              className="pl-10"
            />
          </div>
        </div>

        {/* Test Cases List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : testCases.length === 0 ? (
            <EmptyState
              icon={FileText}
              title="No test cases yet"
              description="Create your first test case to define inputs and expected outputs for evaluation."
              actionLabel="Create Test Case"
              onAction={() => setIsCreateModalOpen(true)}
            />
          ) : (
            <>
              {/* Mobile Card View */}
              <div className="block md:hidden divide-y divide-gray-200">
                {testCases.map((testCase) => (
                  <div
                    key={testCase.id}
                    className="p-4 hover:bg-gray-50 cursor-pointer"
                    onClick={() => openViewModal(testCase)}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-start gap-3 flex-1 min-w-0">
                        <div className="h-9 w-9 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
                          <FileText className="h-5 w-5 text-blue-600" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 mb-1">
                            <Badge variant={testCase.isActive ? "success" : "warning"} className="flex-shrink-0">
                              {testCase.isActive ? "Active" : "Inactive"}
                            </Badge>
                          </div>
                          <p className="text-sm text-gray-900 font-mono line-clamp-2 mb-1">
                            {testCase.question}
                          </p>
                          <p className="text-xs text-gray-400">
                            by {testCase.createdByName}
                          </p>
                        </div>
                      </div>
                      <div className="relative flex-shrink-0">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            setOpenDropdown(openDropdown === testCase.id ? null : testCase.id);
                          }}
                          className="p-1 rounded-lg hover:bg-gray-100"
                        >
                          <MoreVertical className="h-5 w-5 text-gray-400" />
                        </button>

                        {openDropdown === testCase.id && (
                          <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                openViewModal(testCase);
                              }}
                              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                            >
                              <Eye className="h-4 w-4" />
                              View
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                openEditModal(testCase);
                              }}
                              className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                            >
                              <Pencil className="h-4 w-4" />
                              Edit
                            </button>
                            {testCase.isActive ? (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleDelete(testCase);
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
                                  handleReactivate(testCase);
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
                      Question
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Ground Truth
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Created By
                    </th>
                    <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {testCases.map((testCase) => (
                    <tr
                      key={testCase.id}
                      className="hover:bg-gray-50 cursor-pointer"
                      onClick={() => openViewModal(testCase)}
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="h-9 w-9 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
                            <FileText className="h-5 w-5 text-blue-600" />
                          </div>
                          <div className="min-w-0">
                            <p className="text-sm text-gray-900 font-mono truncate max-w-md">
                              {testCase.question}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <p className="text-sm text-gray-500 truncate max-w-xs font-mono">
                          {testCase.groundTruth || "-"}
                        </p>
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant={testCase.isActive ? "success" : "warning"}>
                          {testCase.isActive ? "Active" : "Inactive"}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {testCase.createdByName}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="relative inline-block">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setOpenDropdown(openDropdown === testCase.id ? null : testCase.id);
                            }}
                            className="p-1 rounded-lg hover:bg-gray-100"
                          >
                            <MoreVertical className="h-5 w-5 text-gray-400" />
                          </button>

                          {openDropdown === testCase.id && (
                            <div className="absolute right-0 mt-1 w-48 bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openViewModal(testCase);
                                }}
                                className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                              >
                                <Eye className="h-4 w-4" />
                                View
                              </button>
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  openEditModal(testCase);
                                }}
                                className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                              >
                                <Pencil className="h-4 w-4" />
                                Edit
                              </button>
                              {testCase.isActive ? (
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleDelete(testCase);
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
                                    handleReactivate(testCase);
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
        title="Create Test Case"
        size="lg"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Question <span className="text-red-500">*</span>
            </label>
            <textarea
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              rows={3}
              placeholder="Enter the question/input for evaluation"
              value={formData.question}
              onChange={(e) => setFormData({ ...formData, question: e.target.value })}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Ground Truth (Expected Answer)
            </label>
            <textarea
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              rows={3}
              placeholder="Enter the expected/correct answer"
              value={formData.groundTruth}
              onChange={(e) => setFormData({ ...formData, groundTruth: e.target.value })}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              LLM Output (Pre-generated)
            </label>
            <textarea
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              rows={3}
              placeholder="Enter the LLM's response if already generated"
              value={formData.llmOutput}
              onChange={(e) => setFormData({ ...formData, llmOutput: e.target.value })}
            />
            <p className="mt-1 text-xs text-gray-500">
              <strong>Pre-generated:</strong> Provide output here if you already have the LLM response.{" "}
              <strong>Runtime generation:</strong> Leave empty to generate output using a Client LLM during evaluation (LIVE_GENERATION mode).
            </p>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-sm font-medium text-gray-700">
                Retrieved Context (for RAG)
              </label>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={addContextItem}
                className="text-primary-600 hover:text-primary-700"
              >
                <Plus className="h-4 w-4 mr-1" />
                Add Context
              </Button>
            </div>
            {formData.retrievedContext.length === 0 ? (
              <p className="text-sm text-gray-500 italic py-2">No context chunks added. Click "Add Context" to add one.</p>
            ) : (
              <div className="space-y-2">
                {formData.retrievedContext.map((ctx, idx) => (
                  <div key={idx} className="flex gap-2">
                    <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center text-xs font-medium text-gray-500">
                      {idx + 1}
                    </div>
                    <textarea
                      className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
                      rows={2}
                      placeholder={`Context chunk ${idx + 1}`}
                      value={ctx}
                      onChange={(e) => updateContextItem(idx, e.target.value)}
                    />
                    <button
                      type="button"
                      onClick={() => removeContextItem(idx)}
                      className="flex-shrink-0 p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 pt-4">
            <Button variant="outline" onClick={() => setIsCreateModalOpen(false)} className="w-full sm:w-auto">
              Cancel
            </Button>
            <Button onClick={handleCreate} loading={submitting} className="w-full sm:w-auto">
              Create Test Case
            </Button>
          </div>
        </div>
      </Modal>

      {/* Edit Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Test Case"
        size="lg"
      >
        <div className="space-y-4">
          {formError && <Alert variant="error">{formError}</Alert>}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Question <span className="text-red-500">*</span>
            </label>
            <textarea
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              rows={3}
              placeholder="Enter the question/input for evaluation"
              value={formData.question}
              onChange={(e) => setFormData({ ...formData, question: e.target.value })}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Ground Truth (Expected Answer)
            </label>
            <textarea
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              rows={3}
              placeholder="Enter the expected/correct answer"
              value={formData.groundTruth}
              onChange={(e) => setFormData({ ...formData, groundTruth: e.target.value })}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              LLM Output (Pre-generated)
            </label>
            <textarea
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
              rows={3}
              placeholder="Enter the LLM's response if already generated"
              value={formData.llmOutput}
              onChange={(e) => setFormData({ ...formData, llmOutput: e.target.value })}
            />
            <p className="mt-1 text-xs text-gray-500">
              <strong>Pre-generated:</strong> Provide output here if you already have the LLM response.{" "}
              <strong>Runtime generation:</strong> Leave empty to generate output using a Client LLM during evaluation (LIVE_GENERATION mode).
            </p>
          </div>

          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-sm font-medium text-gray-700">
                Retrieved Context (for RAG)
              </label>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={addContextItem}
                className="text-primary-600 hover:text-primary-700"
              >
                <Plus className="h-4 w-4 mr-1" />
                Add Context
              </Button>
            </div>
            {formData.retrievedContext.length === 0 ? (
              <p className="text-sm text-gray-500 italic py-2">No context chunks added. Click "Add Context" to add one.</p>
            ) : (
              <div className="space-y-2">
                {formData.retrievedContext.map((ctx, idx) => (
                  <div key={idx} className="flex gap-2">
                    <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center text-xs font-medium text-gray-500">
                      {idx + 1}
                    </div>
                    <textarea
                      className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary-200 focus:border-primary-500"
                      rows={2}
                      placeholder={`Context chunk ${idx + 1}`}
                      value={ctx}
                      onChange={(e) => updateContextItem(idx, e.target.value)}
                    />
                    <button
                      type="button"
                      onClick={() => removeContextItem(idx)}
                      className="flex-shrink-0 p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-colors"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
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

      {/* View Modal */}
      <Modal
        isOpen={isViewModalOpen}
        onClose={() => {
          setIsViewModalOpen(false);
          setSelectedTestCase(null);
        }}
        title="Test Case Details"
        size="lg"
      >
        {selectedTestCase && (
          <div className="space-y-4">
            <div className="flex items-center justify-end">
              <Badge variant={selectedTestCase.isActive ? "success" : "warning"}>
                {selectedTestCase.isActive ? "Active" : "Inactive"}
              </Badge>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">Question</label>
              <pre className="bg-gray-50 rounded-lg p-3 text-sm text-gray-900 overflow-x-auto whitespace-pre-wrap">
                {selectedTestCase.question}
              </pre>
            </div>

            {selectedTestCase.groundTruth && (
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">Ground Truth</label>
                <pre className="bg-gray-50 rounded-lg p-3 text-sm text-gray-900 overflow-x-auto whitespace-pre-wrap">
                  {selectedTestCase.groundTruth}
                </pre>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">
                LLM Output
                {selectedTestCase.llmOutput ? (
                  <Badge variant="success" className="ml-2 text-xs">Pre-generated</Badge>
                ) : (
                  <Badge variant="warning" className="ml-2 text-xs">Runtime Generation</Badge>
                )}
              </label>
              {selectedTestCase.llmOutput ? (
                <pre className="bg-gray-50 rounded-lg p-3 text-sm text-gray-900 overflow-x-auto whitespace-pre-wrap">
                  {selectedTestCase.llmOutput}
                </pre>
              ) : (
                <p className="text-sm text-gray-500 italic py-2">
                  No pre-generated output. Will be generated by Client LLM during LIVE_GENERATION evaluation.
                </p>
              )}
            </div>

            {selectedTestCase.retrievedContext && selectedTestCase.retrievedContext.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">
                  Retrieved Context ({selectedTestCase.retrievedContext.length} chunks)
                </label>
                <div className="space-y-2">
                  {selectedTestCase.retrievedContext.map((ctx, idx) => (
                    <pre key={idx} className="bg-gray-50 rounded-lg p-3 text-sm text-gray-900 overflow-x-auto whitespace-pre-wrap">
                      {ctx}
                    </pre>
                  ))}
                </div>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-2">
              <div>
                <p className="text-sm text-gray-500">Created by</p>
                <p className="font-medium text-gray-900">{selectedTestCase.createdByName}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Created at</p>
                <p className="font-medium text-gray-900">
                  {new Date(selectedTestCase.createdAt).toLocaleString()}
                </p>
              </div>
            </div>

            <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 pt-4">
              <Button
                variant="outline"
                onClick={() => {
                  setIsViewModalOpen(false);
                  openEditModal(selectedTestCase);
                }}
                className="w-full sm:w-auto"
              >
                <Pencil className="h-4 w-4 mr-2" />
                Edit
              </Button>
              <Button
                onClick={() => {
                  setIsViewModalOpen(false);
                  setSelectedTestCase(null);
                }}
                className="w-full sm:w-auto"
              >
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </DashboardLayout>
  );
}
