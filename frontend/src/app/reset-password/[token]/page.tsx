"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { Button, Input, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { Eye, EyeOff, CheckCircle, XCircle, KeyRound } from "lucide-react";

interface TokenValidation {
  valid: boolean;
  email?: string;
  message: string;
}

export default function ResetPasswordPage() {
  const params = useParams();
  const token = params.token as string;

  const [tokenValidation, setTokenValidation] = useState<TokenValidation | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    const validateToken = async () => {
      setLoading(true);
      try {
        const response = await api.get<TokenValidation>(
          `/auth/reset-password/${token}`
        );
        if (response.success && response.data) {
          setTokenValidation(response.data);
        } else {
          setError(response.error || "Invalid reset link");
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to validate reset link");
      } finally {
        setLoading(false);
      }
    };

    if (token) {
      validateToken();
    }
  }, [token]);

  const validateForm = (): boolean => {
    if (!password) {
      setFormError("Password is required");
      return false;
    }

    if (password.length < 8) {
      setFormError("Password must be at least 8 characters");
      return false;
    }

    if (!/[A-Z]/.test(password)) {
      setFormError("Password must contain at least one uppercase letter");
      return false;
    }

    if (!/[a-z]/.test(password)) {
      setFormError("Password must contain at least one lowercase letter");
      return false;
    }

    if (!/[0-9]/.test(password)) {
      setFormError("Password must contain at least one number");
      return false;
    }

    if (password !== confirmPassword) {
      setFormError("Passwords do not match");
      return false;
    }

    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);

    try {
      const response = await api.post(`/auth/reset-password/${token}`, {
        password,
        passwordConfirm: confirmPassword,
      });

      if (response.success) {
        setSuccess(true);
      } else {
        setFormError(response.error || "Failed to reset password");
      }
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "An error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  // Loading state
  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-500">Validating reset link...</p>
        </div>
      </div>
    );
  }

  // Error state (invalid token)
  if (error || (tokenValidation && !tokenValidation.valid)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
        <div className="w-full max-w-md">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
            <div className="h-16 w-16 rounded-full bg-red-100 flex items-center justify-center mx-auto mb-4">
              <XCircle className="h-8 w-8 text-red-500" />
            </div>
            <h1 className="text-xl font-bold text-gray-900 mb-2">
              Invalid Reset Link
            </h1>
            <p className="text-gray-500 mb-6">
              {error || tokenValidation?.message || "This password reset link is invalid or has expired."}
            </p>
            <div className="space-y-3">
              <Link href="/forgot-password">
                <Button className="w-full">Request new reset link</Button>
              </Link>
              <Link href="/login">
                <Button variant="ghost" className="w-full">
                  Back to login
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Success state
  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
        <div className="w-full max-w-md">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
            <div className="h-16 w-16 rounded-full bg-green-100 flex items-center justify-center mx-auto mb-4">
              <CheckCircle className="h-8 w-8 text-green-500" />
            </div>
            <h1 className="text-xl font-bold text-gray-900 mb-2">
              Password Reset Successful
            </h1>
            <p className="text-gray-500 mb-6">
              Your password has been reset successfully. You can now sign in with
              your new password.
            </p>
            <Link href="/login">
              <Button className="w-full">Sign in</Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Reset password form
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
      <div className="w-full max-w-md space-y-6">
        {/* Header */}
        <div className="text-center">
          <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-xl bg-primary-600 mb-4">
            <KeyRound className="h-6 w-6 text-white" />
          </div>
          <h1 className="text-2xl font-bold text-gray-900">Reset your password</h1>
          <p className="mt-2 text-sm text-gray-500">
            {tokenValidation?.email
              ? `Enter a new password for ${tokenValidation.email}`
              : "Enter your new password below"}
          </p>
        </div>

        {/* Form */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            {formError && <Alert variant="error">{formError}</Alert>}

            <div className="relative">
              <Input
                label="New Password"
                type={showPassword ? "text" : "password"}
                placeholder="Enter new password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-3 top-[34px] text-gray-400 hover:text-gray-600"
              >
                {showPassword ? (
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
                placeholder="Confirm new password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
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
              Password must be at least 8 characters and contain uppercase,
              lowercase, and a number.
            </p>

            <Button
              type="submit"
              className="w-full"
              size="lg"
              loading={submitting}
            >
              Reset password
            </Button>
          </form>
        </div>

        {/* Footer */}
        <p className="text-center text-sm text-gray-500">
          Remember your password?{" "}
          <Link
            href="/login"
            className="text-primary-600 hover:text-primary-500 font-medium"
          >
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
