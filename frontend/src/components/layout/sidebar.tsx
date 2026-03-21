"use client";

import { Suspense } from "react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  FolderKanban,
  Users,
  Settings,
  LogOut,
  Shield,
  UserPlus,
  UsersRound,
  Play,
  Cpu,
  FileText,
  Database,
  Bot,
  Scale,
  Settings2,
  Activity,
  Webhook,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";

const navigation = [
  { name: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { name: "Projects", href: "/projects", icon: FolderKanban },
  { name: "Test Cases", href: "/testcases", icon: FileText },
  { name: "Datasets", href: "/datasets", icon: Database },
  { name: "Evaluations", href: "/evaluations", icon: Play },
];

const llmNavigation = [
  { name: "Client LLM", href: "/llm-endpoints?type=client", icon: Bot, description: "LLMs being tested" },
  { name: "Judge LLM", href: "/llm-endpoints?type=judge", icon: Scale, description: "LLMs that evaluate" },
];

const otherNavigation = [
  { name: "Teams", href: "/teams", icon: Users },
];

const adminNavigation = [
  { name: "Users", href: "/admin/users", icon: UsersRound },
  { name: "Invitations", href: "/admin/invitations", icon: UserPlus },
  { name: "Settings", href: "/admin/settings", icon: Settings2 },
  { name: "Health", href: "/admin/health", icon: Activity },
  { name: "Webhooks", href: "/admin/webhooks", icon: Webhook },
];

export function Sidebar() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { user, logout } = useAuth();

  return (
    <div className="flex h-screen w-64 flex-col bg-gray-900">
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 px-6 border-b border-gray-800">
        <div className="h-8 w-8 flex items-center justify-center rounded-lg bg-primary-600">
          <svg
            className="h-5 w-5 text-white"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
            />
          </svg>
        </div>
        <span className="text-lg font-semibold text-white">LLMEvalOps</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-3 py-4 overflow-y-auto">
        {navigation.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-gray-800 text-white"
                  : "text-gray-400 hover:bg-gray-800 hover:text-white"
              )}
            >
              <item.icon className="h-5 w-5" />
              {item.name}
            </Link>
          );
        })}

        {/* LLM Section */}
        <div className="pt-4 pb-2">
          <div className="flex items-center gap-2 px-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">
            <Cpu className="h-3 w-3" />
            LLM Configuration
          </div>
        </div>
        {llmNavigation.map((item) => {
          const itemType = item.href.split("type=")[1];
          const currentType = searchParams.get("type");
          const isActive = pathname === "/llm-endpoints" && currentType === itemType;
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-gray-800 text-white"
                  : "text-gray-400 hover:bg-gray-800 hover:text-white"
              )}
              title={item.description}
            >
              <item.icon className="h-5 w-5" />
              {item.name}
            </Link>
          );
        })}

        {/* Other */}
        {otherNavigation.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-gray-800 text-white"
                  : "text-gray-400 hover:bg-gray-800 hover:text-white"
              )}
            >
              <item.icon className="h-5 w-5" />
              {item.name}
            </Link>
          );
        })}

        {/* Admin Section */}
        {user?.role === "ADMIN" && (
          <>
            <div className="pt-4 pb-2">
              <div className="flex items-center gap-2 px-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">
                <Shield className="h-3 w-3" />
                Admin
              </div>
            </div>
            {adminNavigation.map((item) => {
              const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-gray-800 text-white"
                      : "text-gray-400 hover:bg-gray-800 hover:text-white"
                  )}
                >
                  <item.icon className="h-5 w-5" />
                  {item.name}
                </Link>
              );
            })}
          </>
        )}
      </nav>

      {/* User section */}
      <div className="border-t border-gray-800 p-4">
        <div className="flex items-center gap-3 mb-3">
          <div className="h-9 w-9 rounded-full bg-gray-700 flex items-center justify-center">
            <span className="text-sm font-medium text-white">
              {user?.fullName?.charAt(0).toUpperCase()}
            </span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-white truncate">
              {user?.fullName}
            </p>
            <p className="text-xs text-gray-400 truncate">{user?.email}</p>
          </div>
        </div>
        <div className="flex gap-2">
          <Link
            href="/settings"
            className="flex-1 flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm text-gray-400 hover:bg-gray-800 hover:text-white transition-colors"
          >
            <Settings className="h-4 w-4" />
            Settings
          </Link>
          <button
            onClick={logout}
            className="flex items-center justify-center gap-2 rounded-lg px-3 py-2 text-sm text-gray-400 hover:bg-gray-800 hover:text-white transition-colors"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
