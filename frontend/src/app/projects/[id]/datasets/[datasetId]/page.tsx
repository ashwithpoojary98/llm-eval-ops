"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Badge, Alert, Modal, Input, EmptyState } from "@/components/ui";
import { api } from "@/lib/api";
import { Dataset, TestCase, PagedResponse } from "@/lib/types";
import { toast } from "sonner";
import {
  ArrowLeft,
  Database,
  FileText,
  Plus,
  Trash2,
  Search,
  Eye,
} from "lucide-react";

export default function DatasetDetailPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;
  const datasetId = params.datasetId as string;

  const [dataset, setDataset] = useState<Dataset | null>(null);
  const [availableTestCases, setAvailableTestCases] = useState<TestCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Add test cases modal
  const [isAddTestCasesModalOpen, setIsAddTestCasesModalOpen] = useState(false);
  const [selectedTestCaseIds, setSelectedTestCaseIds] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // View test case modal
  const [viewingTestCase, setViewingTestCase] = useState<TestCase | null>(null);

  const fetchDataset = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<Dataset>(`/datasets/${datasetId}`);
      if (response.success && response.data) {
        setDataset(response.data);
      } else {
        setError(response.error || "Failed to fetch dataset");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [datasetId]);

  const fetchAvailableTestCases = async () => {
    try {
      const response = await api.get<PagedResponse<TestCase>>(
        `/projects/${projectId}/testcases?size=100`
      );
      if (response.success && response.data) {
        // Filter out test cases already in the dataset
        const existingIds = dataset?.testCases?.map((tc) => tc.id) || [];
        const filtered = response.data.content.filter(
          (tc) => !existingIds.includes(tc.id) && tc.isActive
        );
        setAvailableTestCases(filtered);
      }
    } catch (err) {
      console.error("Failed to fetch test cases", err);
    }
  };

  useEffect(() => {
    fetchDataset();
  }, [fetchDataset]);

  const handleAddTestCases = async () => {
    if (selectedTestCaseIds.length === 0) {
      return;
    }

    setSubmitting(true);
    try {
      const response = await api.post(`/datasets/${datasetId}/testcases`, {
        testCaseIds: selectedTestCaseIds,
      });

      if (response.success) {
        setIsAddTestCasesModalOpen(false);
        setSelectedTestCaseIds([]);
        toast.success("Test cases added successfully");
        fetchDataset();
      } else {
        toast.error(response.error || "Failed to add test cases");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemoveTestCase = async (testCaseId: string, testCaseQuestion: string) => {
    const preview = testCaseQuestion.length > 50 ? testCaseQuestion.substring(0, 50) + "..." : testCaseQuestion;
    if (!confirm(`Are you sure you want to remove this test case from the dataset?\n\n"${preview}"`)) {
      return;
    }

    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"}/datasets/${datasetId}/testcases`,
        {
          method: "DELETE",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
          },
          body: JSON.stringify({ testCaseIds: [testCaseId] }),
        }
      );

      if (res.ok) {
        toast.success("Test case removed successfully");
        fetchDataset();
      } else {
        let errorMsg = "Failed to remove test case";
        try {
          const data = await res.json();
          errorMsg = data.error || data.message || errorMsg;
        } catch {
          // Ignore JSON parse error
        }
        toast.error(errorMsg);
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    }
  };

  const fetchTestCaseDetails = async (testCaseId: string) => {
    try {
      const response = await api.get<TestCase>(`/testcases/${testCaseId}`);
      if (response.success && response.data) {
        setViewingTestCase(response.data);
      }
    } catch (err) {
      console.error("Failed to fetch test case details", err);
    }
  };

  const filteredAvailableTestCases = availableTestCases.filter(
    (tc) => tc.question.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (loading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      </DashboardLayout>
    );
  }

  if (error && !dataset) {
    return (
      <DashboardLayout>
        <div className="space-y-4">
          <Button variant="ghost" onClick={() => router.back()}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back
          </Button>
          <Alert variant="error">{error}</Alert>
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
            <Button
              variant="ghost"
              size="sm"
              onClick={() => router.push(`/projects/${projectId}/datasets`)}
            >
              <ArrowLeft className="h-4 w-4" />
            </Button>
            <div>
              <div className="flex items-center gap-2 text-sm text-gray-500 mb-1">
                <Link href="/projects" className="hover:text-primary-600">Projects</Link>
                <span>/</span>
                <Link href={`/projects/${projectId}`} className="hover:text-primary-600">
                  {dataset?.projectName}
                </Link>
                <span>/</span>
                <Link href={`/projects/${projectId}/datasets`} className="hover:text-primary-600">
                  Datasets
                </Link>
              </div>
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-purple-100 flex items-center justify-center">
                  <Database className="h-6 w-6 text-purple-600" />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-gray-900">{dataset?.name}</h1>
                  {dataset?.description && (
                    <p className="text-sm text-gray-500">{dataset.description}</p>
                  )}
                </div>
                <Badge variant={dataset?.isActive ? "success" : "warning"}>
                  {dataset?.isActive ? "Active" : "Inactive"}
                </Badge>
              </div>
            </div>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-blue-100 flex items-center justify-center">
                <FileText className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {dataset?.testCaseCount || 0}
                </p>
                <p className="text-sm text-gray-500">Test Cases</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500">Created by</p>
            <p className="font-medium text-gray-900">{dataset?.createdByName}</p>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-4">
            <p className="text-sm text-gray-500">Created at</p>
            <p className="font-medium text-gray-900">
              {dataset?.createdAt && new Date(dataset.createdAt).toLocaleDateString()}
            </p>
          </div>
        </div>

        {/* Test Cases Section */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Test Cases</h2>
            <Button
              size="sm"
              onClick={() => {
                fetchAvailableTestCases();
                setSelectedTestCaseIds([]);
                setSearchQuery("");
                setIsAddTestCasesModalOpen(true);
              }}
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Test Cases
            </Button>
          </div>

          {dataset?.testCases && dataset.testCases.length > 0 ? (
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50">
                  <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase">
                    Question
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
                {dataset.testCases.map((testCase) => (
                  <tr key={testCase.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <FileText className="h-4 w-4 text-gray-400 flex-shrink-0" />
                        <span className="text-sm text-gray-900 font-mono truncate max-w-md">{testCase.question}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={testCase.isActive ? "success" : "warning"}>
                        {testCase.isActive ? "Active" : "Inactive"}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => fetchTestCaseDetails(testCase.id)}
                          className="p-1 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-50"
                          title="View details"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => handleRemoveTestCase(testCase.id, testCase.question)}
                          className="p-1 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-50"
                          title="Remove from dataset"
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
            <EmptyState
              icon={FileText}
              title="No test cases"
              description="Add test cases to this dataset to use them in evaluations."
              actionLabel="Add Test Cases"
              onAction={() => {
                fetchAvailableTestCases();
                setIsAddTestCasesModalOpen(true);
              }}
            />
          )}
        </div>
      </div>

      {/* Add Test Cases Modal */}
      <Modal
        isOpen={isAddTestCasesModalOpen}
        onClose={() => setIsAddTestCasesModalOpen(false)}
        title="Add Test Cases"
        size="lg"
      >
        <div className="space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search test cases..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>

          <div className="border border-gray-200 rounded-lg max-h-96 overflow-y-auto">
            {filteredAvailableTestCases.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                {availableTestCases.length === 0
                  ? "No test cases available. Create test cases first."
                  : "No test cases match your search."}
              </div>
            ) : (
              <div className="divide-y divide-gray-200">
                {filteredAvailableTestCases.map((testCase) => (
                  <label
                    key={testCase.id}
                    className="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      checked={selectedTestCaseIds.includes(testCase.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedTestCaseIds([...selectedTestCaseIds, testCase.id]);
                        } else {
                          setSelectedTestCaseIds(
                            selectedTestCaseIds.filter((id) => id !== testCase.id)
                          );
                        }
                      }}
                      className="h-4 w-4 text-primary-600 rounded border-gray-300 focus:ring-primary-500"
                    />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-gray-900 font-mono truncate">{testCase.question}</p>
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>

          {selectedTestCaseIds.length > 0 && (
            <p className="text-sm text-gray-500">
              {selectedTestCaseIds.length} test case(s) selected
            </p>
          )}

          <div className="flex justify-between pt-4">
            <Link href={`/projects/${projectId}/testcases`}>
              <Button variant="ghost" size="sm">
                <Plus className="h-4 w-4 mr-1" />
                Create New Test Case
              </Button>
            </Link>
            <div className="flex gap-3">
              <Button variant="outline" onClick={() => setIsAddTestCasesModalOpen(false)}>
                Cancel
              </Button>
              <Button
                onClick={handleAddTestCases}
                loading={submitting}
                disabled={selectedTestCaseIds.length === 0}
              >
                Add Selected ({selectedTestCaseIds.length})
              </Button>
            </div>
          </div>
        </div>
      </Modal>

      {/* View Test Case Modal */}
      <Modal
        isOpen={!!viewingTestCase}
        onClose={() => setViewingTestCase(null)}
        title="Test Case Details"
        size="lg"
      >
        {viewingTestCase && (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-500 mb-1">Question</label>
              <pre className="bg-gray-50 rounded-lg p-3 text-sm text-gray-900 overflow-x-auto whitespace-pre-wrap">
                {viewingTestCase.question}
              </pre>
            </div>

            {viewingTestCase.groundTruth && (
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">Ground Truth</label>
                <pre className="bg-gray-50 rounded-lg p-3 text-sm text-gray-900 overflow-x-auto whitespace-pre-wrap">
                  {viewingTestCase.groundTruth}
                </pre>
              </div>
            )}

            {viewingTestCase.llmOutput && (
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">LLM Output</label>
                <pre className="bg-gray-50 rounded-lg p-3 text-sm text-gray-900 overflow-x-auto whitespace-pre-wrap">
                  {viewingTestCase.llmOutput}
                </pre>
              </div>
            )}

            {viewingTestCase.retrievedContext && viewingTestCase.retrievedContext.length > 0 && (
              <div>
                <label className="block text-sm font-medium text-gray-500 mb-1">
                  Retrieved Context ({viewingTestCase.retrievedContext.length} chunks)
                </label>
                <div className="space-y-2">
                  {viewingTestCase.retrievedContext.map((ctx, idx) => (
                    <pre key={idx} className="bg-gray-50 rounded-lg p-3 text-sm text-gray-900 overflow-x-auto whitespace-pre-wrap">
                      {ctx}
                    </pre>
                  ))}
                </div>
              </div>
            )}

            <div className="flex justify-end pt-4">
              <Button variant="outline" onClick={() => setViewingTestCase(null)}>
                Close
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </DashboardLayout>
  );
}
