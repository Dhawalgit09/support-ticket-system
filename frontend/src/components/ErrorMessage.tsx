import { ApiError } from "@/lib/api/client";
import type { FieldError } from "@/lib/types/ticket";

export const NETWORK_ERROR_MESSAGE =
  "Unable to reach the server. Please check your connection and try again.";

export interface ErrorMessageProps {
  message: string;
  fieldErrors?: FieldError[];
  className?: string;
}

export function getDisplayError(error: unknown): ErrorMessageProps {
  if (error instanceof ApiError) {
    return {
      message: error.message,
      fieldErrors: error.fieldErrors,
    };
  }

  if (error instanceof TypeError) {
    return { message: NETWORK_ERROR_MESSAGE };
  }

  return { message: NETWORK_ERROR_MESSAGE };
}

export function ErrorMessage({
  message,
  fieldErrors,
  className = "",
}: ErrorMessageProps) {
  const hasFieldErrors = fieldErrors !== undefined && fieldErrors.length > 0;

  return (
    <div
      role="alert"
      className={`error-banner ${className}`.trim()}
      aria-live="polite"
    >
      <p className="error-banner__message">{message}</p>
      {hasFieldErrors && (
        <ul className="error-banner__list">
          {fieldErrors.map((fieldError) => (
            <li key={fieldError.field}>
              <span className="error-banner__field">{fieldError.field}</span>
              {": "}
              {fieldError.message}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
