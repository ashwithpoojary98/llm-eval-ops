"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { DashboardLayout } from "@/components/layout";
import { useAuth } from "@/lib/auth-context";
import { api } from "@/lib/api";
import { Project, Team, Dataset, EvaluationRunSummary, PagedResponse } from "@/lib/types";
import {
  FolderKanban,
  Users,
  Database,
  Activity,
  ArrowRight,
  Plus,
  Play,
  CheckCircle,
  Clock,
  Loader2,
} from "lucide-react";
import { Button } from "@/components/ui";

export default function DashboardPage() {
  const { user } = useAuth();
  const [projects, setProjects] = useState<Project[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [recentEvaluations, setRecentEvaluations] = useState<EvaluationRunSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalProjects: 0,
    totalTeams: 0,
    totalDatasets: 0,
    totalEvaluations: 0,
  });

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [projectsRes, teamsRes] = await Promise.all([
          api.get<PagedResponse<Project>>("/projects?size=5"),
          api.get<PagedResponse<Team>>("/teams?size=5"),
        ]);

        if (projectsRes.success && projectsRes.data) {
          setProjects(projectsRes.data.content);
          setStats(prev => ({ ...prev, totalProjects: projectsRes.data!.totalElements }));
        }
        if (teamsRes.success && teamsRes.data) {
          setTeams(teamsRes.data.content);
          setStats(prev => ({ ...prev, totalTeams: teamsRes.data!.totalElements }));
        }

        // Fetch datasets and evaluations count from all projects
        if (projectsRes.success && projectsRes.data && projectsRes.data.content.length > 0) {
          let totalDatasets = 0;
          let totalEvaluations = 0;
          const allEvaluations: EvaluationRunSummary[] = [];

          // Fetch datasets and evaluations for each project
          const fetchPromises = projectsRes.data.content.map(async (project) => {
            const [datasetRes, evalRes] = await Promise.all([
              api.get<PagedResponse<Dataset>>(`/projects/${project.id}/datasets?size=1`),
              api.get<PagedResponse<EvaluationRunSummary>>(`/projects/${project.id}/evaluations?size=5`),
            ]);

            if (datasetRes.success && datasetRes.data) {
              totalDatasets += datasetRes.data.totalElements;
            }
            if (evalRes.success && evalRes.data) {
              totalEvaluations += evalRes.data.totalElements;
              allEvaluations.push(...evalRes.data.content);
            }
          });

          await Promise.all(fetchPromises);

          // Sort evaluations by date and take top 5
          const sortedEvaluations = allEvaluations
            .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
            .slice(0, 5);

          setRecentEvaluations(sortedEvaluations);
          setStats(prev => ({ ...prev, totalDatasets, totalEvaluations }));
        }
      } catch (err) {
        console.error("Failed to fetch dashboard data", err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  return (
    <DashboardLayout>
      <div className="space-y-8">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
          <p className="mt-1 text-sm text-gray-500">
            Welcome back, {user?.fullName}
          </p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 rounded-lg bg-blue-100 flex items-center justify-center">
                <FolderKanban className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {loading ? "-" : stats.totalProjects}
                </p>
                <p className="text-sm text-gray-500">Projects</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 rounded-lg bg-green-100 flex items-center justify-center">
                <Users className="h-6 w-6 text-green-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {loading ? "-" : stats.totalTeams}
                </p>
                <p className="text-sm text-gray-500">Teams</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 rounded-lg bg-purple-100 flex items-center justify-center">
                <Database className="h-6 w-6 text-purple-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {loading ? "-" : stats.totalDatasets}
                </p>
                <p className="text-sm text-gray-500">Datasets</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="flex items-center gap-4">
              <div className="h-12 w-12 rounded-lg bg-orange-100 flex items-center justify-center">
                <Activity className="h-6 w-6 text-orange-600" />
              </div>
              <div>
                <p className="text-2xl font-bold text-gray-900">
                  {loading ? "-" : stats.totalEvaluations}
                </p>
                <p className="text-sm text-gray-500">Evaluations</p>
              </div>
            </div>
          </div>
        </div>

        {/* Recent Projects & Teams */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Recent Projects */}
          <div className="bg-white rounded-xl border border-gray-200">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">
                Recent Projects
              </h2>
              <Link href="/projects">
                <Button variant="ghost" size="sm">
                  View All
                  <ArrowRight className="h-4 w-4 ml-1" />
                </Button>
              </Link>
            </div>

            {loading ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
              </div>
            ) : projects.length === 0 ? (
              <div className="px-6 py-8 text-center">
                <FolderKanban className="h-10 w-10 text-gray-300 mx-auto mb-3" />
                <p className="text-sm text-gray-500 mb-4">No projects yet</p>
                <Link href="/projects">
                  <Button size="sm">
                    <Plus className="h-4 w-4 mr-1" />
                    Create Project
                  </Button>
                </Link>
              </div>
            ) : (
              <div className="divide-y divide-gray-100">
                {projects.map((project) => (
                  <Link
                    key={project.id}
                    href={`/projects/${project.id}`}
                    className="block px-6 py-4 hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium text-gray-900">
                          {project.name}
                        </p>
                        <p className="text-sm text-gray-500">
                          {project.description || "No description"}
                        </p>
                      </div>
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded-full ${
                          project.status === "ACTIVE"
                            ? "bg-green-100 text-green-700"
                            : "bg-yellow-100 text-yellow-700"
                        }`}
                      >
                        {project.status}
                      </span>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>

          {/* Recent Teams */}
          <div className="bg-white rounded-xl border border-gray-200">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">
                Your Teams
              </h2>
              <Link href="/teams">
                <Button variant="ghost" size="sm">
                  View All
                  <ArrowRight className="h-4 w-4 ml-1" />
                </Button>
              </Link>
            </div>

            {loading ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
              </div>
            ) : teams.length === 0 ? (
              <div className="px-6 py-8 text-center">
                <Users className="h-10 w-10 text-gray-300 mx-auto mb-3" />
                <p className="text-sm text-gray-500 mb-4">No teams yet</p>
                <Link href="/teams">
                  <Button size="sm">
                    <Plus className="h-4 w-4 mr-1" />
                    Create Team
                  </Button>
                </Link>
              </div>
            ) : (
              <div className="divide-y divide-gray-100">
                {teams.map((team) => (
                  <Link
                    key={team.id}
                    href={`/teams/${team.id}`}
                    className="block px-6 py-4 hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium text-gray-900">{team.name}</p>
                        <p className="text-sm text-gray-500">
                          {team.memberCount ?? team.members?.length ?? 0} members
                        </p>
                      </div>
                      <ArrowRight className="h-4 w-4 text-gray-400" />
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Recent Evaluations */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">
              Recent Evaluations
            </h2>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-8">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary-600"></div>
            </div>
          ) : recentEvaluations.length === 0 ? (
            <div className="px-6 py-8 text-center">
              <Play className="h-10 w-10 text-gray-300 mx-auto mb-3" />
              <p className="text-sm text-gray-500 mb-2">No evaluations yet</p>
              <p className="text-xs text-gray-400">
                Go to a project to start your first evaluation
              </p>
            </div>
          ) : (
            <div className="divide-y divide-gray-100">
              {recentEvaluations.map((evaluation) => (
                <Link
                  key={evaluation.id}
                  href={`/evaluations/${evaluation.id}`}
                  className="block px-6 py-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      {evaluation.status === "COMPLETED" ? (
                        <CheckCircle className="h-5 w-5 text-green-500" />
                      ) : evaluation.status === "RUNNING" ? (
                        <Loader2 className="h-5 w-5 text-blue-500 animate-spin" />
                      ) : (
                        <Clock className="h-5 w-5 text-gray-400" />
                      )}
                      <div>
                        <p className="font-medium text-gray-900">
                          {evaluation.name}
                        </p>
                        <p className="text-sm text-gray-500">
                          {evaluation.datasetName} · Run #{evaluation.runNumber}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-4">
                      {evaluation.overallScore !== undefined && (
                        <span
                          className={`text-sm font-semibold ${
                            evaluation.overallScore >= 0.8
                              ? "text-green-600"
                              : evaluation.overallScore >= 0.6
                              ? "text-yellow-600"
                              : "text-red-600"
                          }`}
                        >
                          {(evaluation.overallScore * 100).toFixed(0)}%
                        </span>
                      )}
                      <span
                        className={`px-2 py-1 text-xs font-medium rounded-full ${
                          evaluation.status === "COMPLETED"
                            ? "bg-green-100 text-green-700"
                            : evaluation.status === "RUNNING"
                            ? "bg-blue-100 text-blue-700"
                            : evaluation.status === "FAILED"
                            ? "bg-red-100 text-red-700"
                            : "bg-gray-100 text-gray-700"
                        }`}
                      >
                        {evaluation.status}
                      </span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Quick Actions */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">
            Quick Actions
          </h2>
          <div className="flex flex-wrap gap-3">
            <Link href="/projects">
              <Button>
                <Plus className="h-4 w-4 mr-2" />
                New Project
              </Button>
            </Link>
            <Link href="/teams">
              <Button variant="outline">
                <Plus className="h-4 w-4 mr-2" />
                New Team
              </Button>
            </Link>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
}
