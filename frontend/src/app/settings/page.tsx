"use client";

import { useState } from "react";
import { DashboardLayout } from "@/components/layout";
import { useAuth } from "@/lib/auth-context";
import { Button, Input, Modal } from "@/components/ui";
import { User, Building2, Shield, Eye, EyeOff } from "lucide-react";
import { api } from "@/lib/api";
import { toast } from "sonner";

export default function SettingsPage() {
  const { user } = useAuth();
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [passwordForm, setPasswordForm] = useState({
    oldPassword: "",
    newPassword: "",
    newPasswordConfirm: "",
  });

  const [validationErrors, setValidationErrors] = useState<{
    oldPassword?: string;
    newPassword?: string;
    newPasswordConfirm?: string;
  }>({});

  const validateForm = (): boolean => {
    const errors: typeof validationErrors = {};

    if (!passwordForm.oldPassword) {
      errors.oldPassword = "Current password is required";
    }

    if (!passwordForm.newPassword) {
      errors.newPassword = "New password is required";
    } else if (passwordForm.newPassword.length < 8) {
      errors.newPassword = "Password must be at least 8 characters";
    }

    if (!passwordForm.newPasswordConfirm) {
      errors.newPasswordConfirm = "Please confirm your new password";
    } else if (passwordForm.newPassword !== passwordForm.newPasswordConfirm) {
      errors.newPasswordConfirm = "Passwords do not match";
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChangePassword = async () => {
    if (!validateForm()) {
      return;
    }

    setSubmitting(true);

    try {
      const response = await api.patch("/auth/update-password", passwordForm);

      if (response.success) {
        toast.success("Password changed successfully");
        setPasswordForm({
          oldPassword: "",
          newPassword: "",
          newPasswordConfirm: "",
        });
        setIsPasswordModalOpen(false);
      } else {
        toast.error(response.error || "Failed to change password");
      }
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  const resetModal = () => {
    setPasswordForm({
      oldPassword: "",
      newPassword: "",
      newPasswordConfirm: "",
    });
    setValidationErrors({});
    setShowOldPassword(false);
    setShowNewPassword(false);
    setShowConfirmPassword(false);
  };

  return (
    <DashboardLayout>
      <div className="space-y-6 max-w-3xl">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
          <p className="mt-1 text-sm text-gray-500">
            Manage your account settings
          </p>
        </div>

        {/* Profile Section */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="px-6 py-4 border-b border-gray-200">
            <div className="flex items-center gap-3">
              <User className="h-5 w-5 text-gray-400" />
              <h2 className="text-lg font-semibold text-gray-900">Profile</h2>
            </div>
          </div>
          <div className="p-6 space-y-4">
            <div className="flex items-center gap-4">
              <div className="h-16 w-16 rounded-full bg-primary-100 flex items-center justify-center">
                <span className="text-2xl font-semibold text-primary-600">
                  {user?.fullName?.charAt(0).toUpperCase()}
                </span>
              </div>
              <div>
                <p className="text-lg font-medium text-gray-900">
                  {user?.fullName}
                </p>
                <p className="text-sm text-gray-500">{user?.email}</p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4">
              <Input
                label="Full Name"
                value={user?.fullName || ""}
                disabled
              />
              <Input
                label="Email"
                value={user?.email || ""}
                disabled
              />
            </div>
          </div>
        </div>

        {/* Organization Section */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="px-6 py-4 border-b border-gray-200">
            <div className="flex items-center gap-3">
              <Building2 className="h-5 w-5 text-gray-400" />
              <h2 className="text-lg font-semibold text-gray-900">
                Organization
              </h2>
            </div>
          </div>
          <div className="p-6 space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Organization Name"
                value={user?.organizationName || ""}
                disabled
              />
              <Input
                label="Your Role"
                value={user?.role || ""}
                disabled
              />
            </div>
          </div>
        </div>

        {/* Security Section */}
        <div className="bg-white rounded-xl border border-gray-200">
          <div className="px-6 py-4 border-b border-gray-200">
            <div className="flex items-center gap-3">
              <Shield className="h-5 w-5 text-gray-400" />
              <h2 className="text-lg font-semibold text-gray-900">Security</h2>
            </div>
          </div>
          <div className="p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-gray-900">Password</p>
                <p className="text-sm text-gray-500">
                  Change your account password
                </p>
              </div>
              <Button
                variant="outline"
                onClick={() => setIsPasswordModalOpen(true)}
              >
                Change Password
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Change Password Modal */}
      <Modal
        isOpen={isPasswordModalOpen}
        onClose={() => {
          setIsPasswordModalOpen(false);
          resetModal();
        }}
        title="Change Password"
      >
        <div className="space-y-4">
          <div className="relative">
            <Input
              label="Current Password"
              type={showOldPassword ? "text" : "password"}
              value={passwordForm.oldPassword}
              onChange={(e) => {
                setPasswordForm({ ...passwordForm, oldPassword: e.target.value });
                if (validationErrors.oldPassword) {
                  setValidationErrors({ ...validationErrors, oldPassword: undefined });
                }
              }}
              error={validationErrors.oldPassword}
              placeholder="Enter your current password"
              required
            />
            <button
              type="button"
              onClick={() => setShowOldPassword(!showOldPassword)}
              className="absolute right-3 top-[34px] text-gray-400 hover:text-gray-600"
            >
              {showOldPassword ? (
                <EyeOff className="h-5 w-5" />
              ) : (
                <Eye className="h-5 w-5" />
              )}
            </button>
          </div>

          <div className="relative">
            <Input
              label="New Password"
              type={showNewPassword ? "text" : "password"}
              value={passwordForm.newPassword}
              onChange={(e) => {
                setPasswordForm({ ...passwordForm, newPassword: e.target.value });
                if (validationErrors.newPassword) {
                  setValidationErrors({ ...validationErrors, newPassword: undefined });
                }
              }}
              error={validationErrors.newPassword}
              placeholder="Enter your new password"
              required
            />
            <button
              type="button"
              onClick={() => setShowNewPassword(!showNewPassword)}
              className="absolute right-3 top-[34px] text-gray-400 hover:text-gray-600"
            >
              {showNewPassword ? (
                <EyeOff className="h-5 w-5" />
              ) : (
                <Eye className="h-5 w-5" />
              )}
            </button>
          </div>

          <div className="relative">
            <Input
              label="Confirm New Password"
              type={showConfirmPassword ? "text" : "password"}
              value={passwordForm.newPasswordConfirm}
              onChange={(e) => {
                setPasswordForm({ ...passwordForm, newPasswordConfirm: e.target.value });
                if (validationErrors.newPasswordConfirm) {
                  setValidationErrors({ ...validationErrors, newPasswordConfirm: undefined });
                }
              }}
              error={validationErrors.newPasswordConfirm}
              placeholder="Confirm your new password"
              required
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-3 top-[34px] text-gray-400 hover:text-gray-600"
            >
              {showConfirmPassword ? (
                <EyeOff className="h-5 w-5" />
              ) : (
                <Eye className="h-5 w-5" />
              )}
            </button>
          </div>

          <p className="text-xs text-gray-500">
            Password must be at least 8 characters long.
          </p>

          <div className="flex justify-end gap-3 pt-4">
            <Button
              variant="outline"
              onClick={() => {
                setIsPasswordModalOpen(false);
                resetModal();
              }}
            >
              Cancel
            </Button>
            <Button
              onClick={handleChangePassword}
              loading={submitting}
            >
              Update Password
            </Button>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  );
}
