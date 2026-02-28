"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Badge, EmptyState, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { Dataset, PagedResponse } from "@/lib/types";
import {
  Search,
  Database,
  Eye,
  FolderKanban,
  FileText,
} from "lucide-react";

export default function GlobalDatasetsPage() {
  const router = useRouter();

  const [datasets, setDatasets] = useState<Dataset[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchDatasets = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get<PagedResponse<Dataset>>(
        `/datasets?page=${page}&size=20&search=${encodeURIComponent(searchQuery)}`
      );
      if (response.success && response.data) {
        setDatasets(response.data.content);
        setTotalPages(response.data.totalPages);
      } else {
        setError(response.error || "Failed to fetch datasets");
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setLoading(false);
    }
  }, [page, searchQuery]);

  useEffect(() => {
    fetchDatasets();
  }, [fetchDatasets]);

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Datasets</h1>
            <p className="mt-1 text-sm text-gray-500">
              All datasets across your projects
            </p>
          </div>
        </div>

        {/* Search */}
        <div className="flex items-center gap-4">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
            <Input
              placeholder="Search datasets..."
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

        {/* Datasets List */}
        <div className="bg-white rounded-xl border border-gray-200">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
            </div>
          ) : datasets.length === 0 ? (
            <EmptyState
              icon={Database}
              title="No datasets yet"
              description="Datasets are created within projects. Go to a project to create datasets."
              actionLabel="View Projects"
              onAction={() => router.push("/projects")}
            />
          ) : (
            <>
              <table className="w-full">
                <thead>
                  <tr className="border-b border-gray-200 bg-gray-50">
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Dataset
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Project
                    </th>
                    <th className="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Test Cases
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
                  {datasets.map((dataset) => (
                    <tr key={dataset.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="h-9 w-9 rounded-lg bg-purple-100 flex items-center justify-center flex-shrink-0">
                            <Database className="h-5 w-5 text-purple-600" />
                          </div>
                          <div>
                            <p className="text-sm font-medium text-gray-900">
                              {dataset.name}
                            </p>
                            {dataset.description && (
                              <p className="text-xs text-gray-500 truncate max-w-xs">
                                {dataset.description}
                              </p>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <Link
                          href={`/projects/${dataset.projectId}`}
                          className="flex items-center gap-2 text-sm text-primary-600 hover:text-primary-700"
                        >
                          <FolderKanban className="h-4 w-4" />
                          {dataset.projectName}
                        </Link>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2 text-sm text-gray-600">
                          <FileText className="h-4 w-4" />
                          {dataset.testCaseCount}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <Badge variant={dataset.isActive ? "success" : "warning"}>
                          {dataset.isActive ? "Active" : "Inactive"}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-sm text-gray-500">
                        {dataset.createdByName}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Link
                          href={`/projects/${dataset.projectId}/datasets/${dataset.id}`}
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
