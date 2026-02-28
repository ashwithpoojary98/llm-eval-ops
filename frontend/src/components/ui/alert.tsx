import { ReactNode } from "react";
import { cn } from "@/lib/utils";
import { AlertCircle, CheckCircle, Info, XCircle } from "lucide-react";

interface AlertProps {
  variant?: "info" | "success" | "warning" | "error";
  title?: string;
  children: ReactNode;
  className?: string;
}

const icons = {
  info: Info,
  success: CheckCircle,
  warning: AlertCircle,
  error: XCircle,
};

const styles = {
  info: "bg-blue-50 text-blue-800 border-blue-200",
  success: "bg-green-50 text-green-800 border-green-200",
  warning: "bg-yellow-50 text-yellow-800 border-yellow-200",
  error: "bg-red-50 text-red-800 border-red-200",
};

const iconStyles = {
  info: "text-blue-500",
  success: "text-green-500",
  warning: "text-yellow-500",
  error: "text-red-500",
};

export function Alert({
  variant = "info",
  title,
  children,
  className,
}: AlertProps) {
  const Icon = icons[variant];

  return (
    <div
      className={cn(
        "flex gap-3 rounded-lg border p-4",
        styles[variant],
        className
      )}
      role="alert"
    >
      <Icon className={cn("h-5 w-5 flex-shrink-0", iconStyles[variant])} />
      <div className="flex-1">
        {title && <h5 className="font-medium mb-1">{title}</h5>}
        <div className="text-sm">{children}</div>
      </div>
    </div>
  );
}
