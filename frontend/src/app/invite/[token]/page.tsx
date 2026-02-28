"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { Button, Input, Alert } from "@/components/ui";
import { api } from "@/lib/api";
import { InvitationDetails } from "@/lib/types";
import { Eye, EyeOff, CheckCircle, XCircle, Clock, Building2, User, Shield } from "lucide-react";
import Link from "next/link";

export default function AcceptInvitationPage() {
  const params = useParams();
  const router = useRouter();
  const token = params.token as string;

  const [invitation, setInvitation] = useState<InvitationDetails | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [fullName, setFullName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    const fetchInvitation = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await api.get<InvitationDetails>(`/invitations/${token}`);
        if (response.success && response.data) {
          setInvitation(response.data);

          // Check if invitation is expired or already used
          if (response.data.status !== "PENDING") {
            if (response.data.status === "ACCEPTED") {
              setError("This invitation has already been accepted.");
            } else if (response.data.status === "EXPIRED") {
              setError("This invitation has expired.");
            } else if (response.data.status === "REVOKED") {
              setError("This invitation has been revoked.");
            }
          } else if (new Date(response.data.expiresAt) < new Date()) {
            setError("This invitation has expired.");
          }
        } else {
          setError(response.error || "Invalid or expired invitation link.");
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load invitation.");
      } finally {
        setLoading(false);
      }
    };

    if (token) {
      fetchInvitation();
    }
  }, [token]);

  const validateForm = (): boolean => {
    if (!fullName.trim()) {
      setFormError("Full name is required");
      return false;
    }

    if (fullName.trim().length < 2) {
      setFormError("Full name must be at least 2 characters");
      return false;
    }

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
      const request = {
        fullName: fullName.trim(),
        password,
        passwordConfirm: confirmPassword,
      };

      const response = await api.post(`/invitations/${token}/accept`, request);

      if (response.success) {
        setSuccess(true);
      } else {
        setFormError(response.error || "Failed to accept invitation");
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
          <p className="text-gray-500">Loading invitation...</p>
        </div>
      </div>
    );
  }

  // Error state (invalid/expired invitation)
  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
        <div className="w-full max-w-md">
          <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-8 text-center">
            <div className="h-16 w-16 rounded-full bg-red-100 flex items-center justify-center mx-auto mb-4">
              <XCircle className="h-8 w-8 text-red-500" />
            </div>
            <h1 className="text-xl font-bold text-gray-900 mb-2">
              Invalid Invitation
            </h1>
            <p className="text-gray-500 mb-6">{error}</p>
            <Link href="/login">
              <Button variant="outline">Go to Login</Button>
            </Link>
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
              Welcome to LLMEvalOps!
            </h1>
            <p className="text-gray-500 mb-6">
              Your account has been created successfully. You can now sign in with
              your email and password.
            </p>
            <Link href="/login">
              <Button className="w-full">Sign In</Button>
            </Link>
          </div>
        </div>
      </div>
    );
  }

  // Invitation form
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4">
      <div className="w-full max-w-md space-y-6">
        {/* Header */}
        <div className="text-center">
          <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-xl bg-primary-600 mb-4">
            <svg
              className="h-8 w-8 text-white"
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
          <h1 className="text-2xl font-bold text-gray-900">
            You&apos;ve been invited!
          </h1>
          <p className="mt-2 text-sm text-gray-500">
            Complete your registration to join the team
          </p>
        </div>

        {/* Invitation Details Card */}
        {invitation && (
          <div className="bg-white rounded-xl border border-gray-200 p-6">
            <div className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-blue-100 flex items-center justify-center">
                  <Building2 className="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <p className="text-xs text-gray-500">Organization</p>
                  <p className="font-medium text-gray-900">
                    {invitation.organizationName}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-green-100 flex items-center justify-center">
                  <User className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-xs text-gray-500">Invited by</p>
                  <p className="font-medium text-gray-900">
                    {invitation.invitedByName}
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-purple-100 flex items-center justify-center">
                  <Shield className="h-5 w-5 text-purple-600" />
                </div>
                <div>
                  <p className="text-xs text-gray-500">Your Role</p>
                  <p className="font-medium text-gray-900">{invitation.role}</p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="h-10 w-10 rounded-lg bg-orange-100 flex items-center justify-center">
                  <Clock className="h-5 w-5 text-orange-600" />
                </div>
                <div>
                  <p className="text-xs text-gray-500">Expires</p>
                  <p className="font-medium text-gray-900">
                    {new Date(invitation.expiresAt).toLocaleDateString("en-US", {
                      year: "numeric",
                      month: "long",
                      day: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Registration Form */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            {formError && <Alert variant="error">{formError}</Alert>}

            <Input
              label="Email Address"
              type="email"
              value={invitation?.email || ""}
              disabled
              className="bg-gray-50"
            />

            <Input
              label="Full Name"
              type="text"
              placeholder="Enter your full name"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
            />

            <div className="relative">
              <Input
                label="Password"
                type={showPassword ? "text" : "password"}
                placeholder="Create a password"
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
                label="Confirm Password"
                type={showConfirmPassword ? "text" : "password"}
                placeholder="Confirm your password"
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
              Create Account
            </Button>
          </form>
        </div>

        {/* Footer */}
        <p className="text-center text-sm text-gray-500">
          Already have an account?{" "}
          <Link href="/login" className="text-primary-600 hover:text-primary-500 font-medium">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
