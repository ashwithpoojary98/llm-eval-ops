"use client";

import { useState, useEffect, useCallback } from "react";
import { DashboardLayout } from "@/components/layout";
import { Button, Input, Modal, Badge, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import {
  EmailConfigResponse,
  TestEmailResponse,
  PlatformSettingsResponse,
} from "@/lib/types";
import {
  Mail,
  Eye,
  EyeOff,
  CheckCircle,
  XCircle,
  Settings2,
  Globe,
} from "lucide-react";
import { toast } from "sonner";

type Tab = "email" | "platform";

export default function AdminSettingsPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<Tab>("email");

  // Email config state
  const [emailConfig, setEmailConfig] = useState<EmailConfigResponse | null>(null);
  const [emailLoading, setEmailLoading] = useState(true);
  const [emailSaving, setEmailSaving] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [testEmail, setTestEmail] = useState("");
  const [testSending, setTestSending] = useState(false);
  const [testResult, setTestResult] = useState<TestEmailResponse | null>(null);
  const [togglingEmail, setTogglingEmail] = useState(false);

  // Email form fields
  const [smtpHost, setSmtpHost] = useState("");
  const [smtpPort, setSmtpPort] = useState("587");
  const [smtpUsername, setSmtpUsername] = useState("");
  const [smtpPassword, setSmtpPassword] = useState("");
  const [fromAddress, setFromAddress] = useState("");
  const [fromName, setFromName] = useState("");
  const [enableTls, setEnableTls] = useState(true);
  const [enableSsl, setEnableSsl] = useState(false);
  const [connectionTimeoutMs, setConnectionTimeoutMs] = useState("30000");

  // Platform settings state
  const [platformSettings, setPlatformSettings] = useState<PlatformSettingsResponse | null>(null);
  const [platformLoading, setPlatformLoading] = useState(true);
  const [platformSaving, setPlatformSaving] = useState(false);

  // Platform form fields
  const [platformName, setPlatformName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [supportEmail, setSupportEmail] = useState("");

  const fetchEmailConfig = useCallback(async () => {
    setEmailLoading(true);
    try {
      const response = await api.get<EmailConfigResponse>("/admin/settings/email");
      if (response.success && response.data) {
        const cfg = response.data;
        setEmailConfig(cfg);
        setSmtpHost(cfg.smtpHost ?? "");
        setSmtpPort(cfg.smtpPort?.toString() ?? "587");
        setSmtpUsername(cfg.smtpUsername ?? "");
        setFromAddress(cfg.fromAddress ?? "");
        setFromName(cfg.fromName ?? "");
        setEnableTls(cfg.enableTls);
        setEnableSsl(cfg.enableSsl);
        setConnectionTimeoutMs(cfg.connectionTimeoutMs?.toString() ?? "30000");
      } else {
        toast.error(response.error || "Failed to fetch email configuration");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setEmailLoading(false);
    }
  }, []);

  const fetchPlatformSettings = useCallback(async () => {
    setPlatformLoading(true);
    try {
      const response = await api.get<PlatformSettingsResponse>("/admin/settings/platform");
      if (response.success && response.data) {
        const cfg = response.data;
        setPlatformSettings(cfg);
        setPlatformName(cfg.platformName ?? "");
        setBaseUrl(cfg.baseUrl ?? "");
        setSupportEmail(cfg.supportEmail ?? "");
      } else {
        toast.error(response.error || "Failed to fetch platform settings");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setPlatformLoading(false);
    }
  }, []);

  useEffect(() => {
    if (user?.role === "ADMIN") {
      fetchEmailConfig();
      fetchPlatformSettings();
    }
  }, [fetchEmailConfig, fetchPlatformSettings, user?.role]);

  if (user?.role !== "ADMIN") {
    return (
      <DashboardLayout>
        <Alert variant="error">
          You do not have permission to access this page.
        </Alert>
      </DashboardLayout>
    );
  }

  const handleSaveEmail = async () => {
    setEmailSaving(true);
    try {
      const body: Record<string, unknown> = {
        smtpHost,
        smtpPort: parseInt(smtpPort, 10),
        smtpUsername,
        fromAddress,
        fromName,
        enableTls,
        enableSsl,
        connectionTimeoutMs: parseInt(connectionTimeoutMs, 10),
      };
      if (smtpPassword) {
        body.smtpPassword = smtpPassword;
      }
      const response = await api.put<EmailConfigResponse>("/admin/settings/email", body);
      if (response.success) {
        toast.success("Email configuration saved");
        setSmtpPassword("");
        fetchEmailConfig();
      } else {
        toast.error(response.error || "Failed to save email configuration");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setEmailSaving(false);
    }
  };

  const handleToggleEmail = async () => {
    if (!emailConfig) return;
    setTogglingEmail(true);
    try {
      const body = { emailEnabled: !emailConfig.emailEnabled };
      const response = await api.put<EmailConfigResponse>("/admin/settings/email", body);
      if (response.success) {
        toast.success(
          emailConfig.emailEnabled ? "Email notifications disabled" : "Email notifications enabled"
        );
        fetchEmailConfig();
      } else {
        toast.error(response.error || "Failed to toggle email");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setTogglingEmail(false);
    }
  };

  const handleSendTestEmail = async () => {
    if (!testEmail) {
      toast.error("Please enter a recipient email");
      return;
    }
    setTestSending(true);
    setTestResult(null);
    try {
      const response = await api.post<TestEmailResponse>("/admin/settings/email/test", {
        recipientEmail: testEmail,
      });
      if (response.success && response.data) {
        setTestResult(response.data);
        if (response.data.success) {
          toast.success("Test email sent successfully");
        } else {
          toast.error("Test email failed");
        }
      } else {
        toast.error(response.error || "Failed to send test email");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setTestSending(false);
    }
  };

  const handleSavePlatform = async () => {
    setPlatformSaving(true);
    try {
      const response = await api.put<PlatformSettingsResponse>("/admin/settings/platform", {
        platformName,
        baseUrl,
        supportEmail,
      });
      if (response.success) {
        toast.success("Platform settings saved");
        fetchPlatformSettings();
      } else {
        toast.error(response.error || "Failed to save platform settings");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setPlatformSaving(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
          <p className="mt-1 text-sm text-gray-500">
            Manage platform-wide configuration
          </p>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200">
          <nav className="-mb-px flex gap-6">
            <button
              onClick={() => setActiveTab("email")}
              className={
                activeTab === "email"
                  ? "border-b-2 border-primary-600 text-primary-600 pb-3 text-sm font-medium"
                  : "text-gray-500 hover:text-gray-700 pb-3 text-sm font-medium"
              }
            >
              <div className="flex items-center gap-2">
                <Mail className="h-4 w-4" />
                Email Configuration
              </div>
            </button>
            <button
              onClick={() => setActiveTab("platform")}
              className={
                activeTab === "platform"
                  ? "border-b-2 border-primary-600 text-primary-600 pb-3 text-sm font-medium"
                  : "text-gray-500 hover:text-gray-700 pb-3 text-sm font-medium"
              }
            >
              <div className="flex items-center gap-2">
                <Globe className="h-4 w-4" />
                Platform Settings
              </div>
            </button>
          </nav>
        </div>

        {/* Email Configuration Tab */}
        {activeTab === "email" && (
          <div className="space-y-6">
            {emailLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
              </div>
            ) : (
              <>
                {/* Status banner */}
                {emailConfig && !emailConfig.configured && (
                  <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-start gap-3">
                    <XCircle className="h-5 w-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                    <div>
                      <p className="text-sm font-medium text-yellow-800">Email Not Configured</p>
                      <p className="text-sm text-yellow-700 mt-1">
                        Configure your SMTP settings below to enable email notifications.
                      </p>
                    </div>
                  </div>
                )}

                {/* Email enabled status */}
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div>
                        <h2 className="text-lg font-semibold text-gray-900">Email Notifications</h2>
                        <p className="text-sm text-gray-500 mt-0.5">
                          Enable or disable all email notifications from the platform
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      {emailConfig?.emailEnabled ? (
                        <Badge variant="success">Email Enabled</Badge>
                      ) : (
                        <Badge variant="error">Email Disabled</Badge>
                      )}
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={handleToggleEmail}
                        loading={togglingEmail}
                      >
                        {emailConfig?.emailEnabled ? "Disable" : "Enable"}
                      </Button>
                    </div>
                  </div>
                </div>

                {/* SMTP Configuration Form */}
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-6">SMTP Configuration</h2>
                  <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
                    <div className="sm:col-span-2">
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        SMTP Host
                      </label>
                      <Input
                        placeholder="smtp.example.com"
                        value={smtpHost}
                        onChange={(e) => setSmtpHost(e.target.value)}
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        SMTP Port
                      </label>
                      <Input
                        type="number"
                        placeholder="587"
                        value={smtpPort}
                        onChange={(e) => setSmtpPort(e.target.value)}
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        SMTP Username
                      </label>
                      <Input
                        placeholder="username@example.com"
                        value={smtpUsername}
                        onChange={(e) => setSmtpUsername(e.target.value)}
                      />
                    </div>

                    <div className="sm:col-span-2">
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        SMTP Password
                      </label>
                      <div className="relative">
                        <Input
                          type={showPassword ? "text" : "password"}
                          placeholder={emailConfig?.hasPassword ? "Leave blank to keep existing" : "Enter password"}
                          value={smtpPassword}
                          onChange={(e) => setSmtpPassword(e.target.value)}
                          className="pr-10"
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword((p) => !p)}
                          className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                        >
                          {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </button>
                      </div>
                      {emailConfig?.hasPassword && (
                        <p className="mt-1 text-xs text-gray-500">A password is currently saved. Leave blank to keep it.</p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        From Email
                      </label>
                      <Input
                        placeholder="noreply@example.com"
                        value={fromAddress}
                        onChange={(e) => setFromAddress(e.target.value)}
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        From Name
                      </label>
                      <Input
                        placeholder="LLMEvalOps"
                        value={fromName}
                        onChange={(e) => setFromName(e.target.value)}
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Connection Timeout (ms)
                      </label>
                      <Input
                        type="number"
                        placeholder="30000"
                        value={connectionTimeoutMs}
                        onChange={(e) => setConnectionTimeoutMs(e.target.value)}
                      />
                    </div>

                    <div className="flex items-start gap-6 pt-2">
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={enableTls}
                          onChange={(e) => setEnableTls(e.target.checked)}
                          className="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                        />
                        <span className="text-sm font-medium text-gray-700">Enable STARTTLS</span>
                      </label>
                      <label className="flex items-center gap-2 cursor-pointer">
                        <input
                          type="checkbox"
                          checked={enableSsl}
                          onChange={(e) => setEnableSsl(e.target.checked)}
                          className="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                        />
                        <span className="text-sm font-medium text-gray-700">Enable SSL</span>
                      </label>
                    </div>
                  </div>

                  <div className="flex justify-end mt-6 pt-4 border-t border-gray-100">
                    <Button onClick={handleSaveEmail} loading={emailSaving}>
                      Save Configuration
                    </Button>
                  </div>
                </div>

                {/* Test Email Section */}
                <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                  <h2 className="text-lg font-semibold text-gray-900 mb-1">Send Test Email</h2>
                  <p className="text-sm text-gray-500 mb-5">
                    Verify your SMTP settings by sending a test email.
                  </p>

                  <div className="flex items-end gap-3">
                    <div className="flex-1">
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Recipient Email
                      </label>
                      <Input
                        type="email"
                        placeholder="you@example.com"
                        value={testEmail}
                        onChange={(e) => setTestEmail(e.target.value)}
                      />
                    </div>
                    <Button
                      onClick={handleSendTestEmail}
                      loading={testSending}
                      variant="outline"
                    >
                      <Mail className="h-4 w-4 mr-2" />
                      Send Test
                    </Button>
                  </div>

                  {testResult && (
                    <div
                      className={`mt-4 rounded-lg p-4 flex items-start gap-3 ${
                        testResult.success
                          ? "bg-green-50 border border-green-200"
                          : "bg-red-50 border border-red-200"
                      }`}
                    >
                      {testResult.success ? (
                        <CheckCircle className="h-5 w-5 text-green-600 flex-shrink-0 mt-0.5" />
                      ) : (
                        <XCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
                      )}
                      <div className="min-w-0">
                        <p
                          className={`text-sm font-medium ${
                            testResult.success ? "text-green-800" : "text-red-800"
                          }`}
                        >
                          {testResult.message}
                        </p>
                        {testResult.errorDetail && (
                          <p className="text-sm text-red-700 mt-1 font-mono break-all">
                            {testResult.errorDetail}
                          </p>
                        )}
                        <p
                          className={`text-xs mt-1 ${
                            testResult.success ? "text-green-600" : "text-red-600"
                          }`}
                        >
                          Latency: {testResult.latencyMs}ms
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        )}

        {/* Platform Settings Tab */}
        {activeTab === "platform" && (
          <div className="space-y-6">
            {platformLoading ? (
              <div className="flex items-center justify-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
              </div>
            ) : (
              <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
                <div className="flex items-center gap-2 mb-6">
                  <Settings2 className="h-5 w-5 text-gray-500" />
                  <h2 className="text-lg font-semibold text-gray-900">Platform Settings</h2>
                </div>

                <div className="space-y-5">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Platform Name
                    </label>
                    <Input
                      placeholder="LLMEvalOps"
                      value={platformName}
                      onChange={(e) => setPlatformName(e.target.value)}
                    />
                    <p className="mt-1 text-xs text-gray-500">
                      The name displayed throughout the platform
                    </p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Base URL
                    </label>
                    <Input
                      placeholder="https://evalops.example.com"
                      value={baseUrl}
                      onChange={(e) => setBaseUrl(e.target.value)}
                    />
                    <p className="mt-1 text-xs text-gray-500">
                      The frontend URL used in email links and notifications
                    </p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Support Email
                    </label>
                    <Input
                      type="email"
                      placeholder="support@example.com"
                      value={supportEmail}
                      onChange={(e) => setSupportEmail(e.target.value)}
                    />
                    <p className="mt-1 text-xs text-gray-500">
                      Shown in emails and error pages as the contact address
                    </p>
                  </div>
                </div>

                <div className="flex justify-end mt-6 pt-4 border-t border-gray-100">
                  <Button onClick={handleSavePlatform} loading={platformSaving}>
                    Save Settings
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
