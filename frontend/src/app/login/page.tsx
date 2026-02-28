"use client";

import { useState, FormEvent, useEffect, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/auth-context";
import { Button, Input, Alert } from "@/components/ui";
import { Eye, EyeOff } from "lucide-react";

function LoginForm() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [sessionExpired, setSessionExpired] = useState(false);
  const [validationErrors, setValidationErrors] = useState<{
    email?: string;
    password?: string;
  }>({});

  const { login, loading, error, clearError, user } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();

  // Check for session expired parameter
  useEffect(() => {
    if (searchParams.get("expired") === "true") {
      setSessionExpired(true);
      // Clean up the URL
      router.replace("/login", { scroll: false });
    }
  }, [searchParams, router]);

  // Redirect if already logged in
  useEffect(() => {
    if (user) {
      router.replace("/dashboard");
    }
  }, [user, router]);

  const validateForm = (): boolean => {
    const errors: { email?: string; password?: string } = {};

    if (!email) {
      errors.email = "Email is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errors.email = "Please enter a valid email address";
    }

    if (!password) {
      errors.password = "Password is required";
    } else if (password.length < 6) {
      errors.password = "Password must be at least 6 characters";
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    clearError();
    setSessionExpired(false);

    if (!validateForm()) {
      return;
    }

    const success = await login({ email, password });
    if (success) {
      router.push("/dashboard");
    }
  };

  return (
    <div className="bg-white py-8 px-6 shadow-sm rounded-xl border border-gray-200">
      <form className="space-y-6" onSubmit={handleSubmit}>
        {sessionExpired && (
          <Alert variant="warning">
            Your session has expired. Please sign in again.
          </Alert>
        )}

        {error && (
          <Alert variant="error">
            {error}
          </Alert>
        )}

        <Input
          label="Email address"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            if (validationErrors.email) {
              setValidationErrors((prev) => ({ ...prev, email: undefined }));
            }
          }}
          error={validationErrors.email}
          placeholder="you@example.com"
        />

        <div className="relative">
          <Input
            label="Password"
            type={showPassword ? "text" : "password"}
            autoComplete="current-password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              if (validationErrors.password) {
                setValidationErrors((prev) => ({
                  ...prev,
                  password: undefined,
                }));
              }
            }}
            error={validationErrors.password}
            placeholder="Enter your password"
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

        <div className="flex justify-end">
          <Link
            href="/forgot-password"
            className="text-sm text-primary-600 hover:text-primary-500"
          >
            Forgot password?
          </Link>
        </div>

        <Button
          type="submit"
          className="w-full"
          size="lg"
          loading={loading}
        >
          Sign in
        </Button>
      </form>
    </div>
  );
}

function LoginFormFallback() {
  return (
    <div className="bg-white py-8 px-6 shadow-sm rounded-xl border border-gray-200">
      <div className="space-y-6 animate-pulse">
        <div className="h-16 bg-gray-200 rounded" />
        <div className="h-16 bg-gray-200 rounded" />
        <div className="h-10 bg-gray-200 rounded" />
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="w-full max-w-md space-y-8">
        {/* Header */}
        <div className="text-center">
          <div className="mx-auto h-12 w-12 flex items-center justify-center rounded-xl bg-primary-600">
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
          <h2 className="mt-6 text-3xl font-bold tracking-tight text-gray-900">
            LLMEvalOps
          </h2>
          <p className="mt-2 text-sm text-gray-600">
            Sign in to your account to continue
          </p>
        </div>

        {/* Form wrapped in Suspense */}
        <Suspense fallback={<LoginFormFallback />}>
          <LoginForm />
        </Suspense>

        {/* Footer */}
        <p className="text-center text-sm text-gray-500">
          LLM/RAG Evaluation Platform
        </p>
      </div>
    </div>
  );
}
