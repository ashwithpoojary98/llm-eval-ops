"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Badge, EmptyState, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { TestCase, PagedResponse } from "@/lib/types";
import {
  Search,
  FileText,
  Eye,
  FolderKanban,
} from "lucide-react";

export default function GlobalTestCasesPage() {
  const router = useRouter();

  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchTestCases = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<PagedResponse<TestCase>>(
        `/testcases?page=${page}&size=20&search=${encodeURIComponent(searchQuery)}`
      );
      if (response.success && response.data) {
        setTestCases(response.data.content);
        setTotalPages(response.data.totalPages);
      } else {
        setError(response.error || "Failed to fetch test cases");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [page, searchQuery]);

  useEffect(() => {
    fetchTestCases();
  }, [fetchTestCases]);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Test Cases</h1>
            <p className="mt-1 text-sm text-gray-500">
              All test cases across your projects
            </p>
          </div>
        </div>

        {/* Search */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-md">
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

        {/* Error */}
        {error && <Alert variant="error">{error}</Alert>}

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
              description="Test cases are created within projects. Go to a project to create test cases."
              actionLabel="View Projects"
              onAction={() => router.push("/projects")}
            />
          ) : (
            <>
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Question
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Project
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      LLM Output
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="text-right px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200">
                  {testCases.map((testCase) => (
                    <tr key={testCase.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="h-9 w-9 rounded-lg bg-blue-100 flex items-center justify-center flex-shrink-0">
                            <FileText className="h-5 w-5 text-blue-600" />
                          </div>
                          <p className="text-sm text-gray-900 font-mono truncate max-w-md">
                            {testCase.question}
                          </p>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <Link
                          href={`/projects/${testCase.projectId}`}
                          className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700"
                        >
                          <FolderKanban className="h-4 w-4" />
                          {testCase.projectName || "View Project"}
                        </Link>
                      </td>
                      <td className="px-6 py-4">
                        {testCase.llmOutput ? (
                          <Badge variant="success" className="text-xs">Pre-generated</Badge>
                        ) : (
                          <Badge variant="warning" className="text-xs">Runtime</Badge>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant={testCase.isActive ? "success" : "warning"}>
                          {testCase.isActive ? "Active" : "Inactive"}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Link
                          href={`/projects/${testCase.projectId}/testcases`}
                          className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm text-primary-600 hover:bg-primary-50 transition-colors"
                        >
                          <Eye className="h-4 w-4" />
                          View
                        </Link>
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
    </DashboardLayout>
  );
}
