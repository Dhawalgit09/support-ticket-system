import type { ApiErrorBody } from "@/lib/types/ticket";

export class ApiError extends Error {
  readonly status: number;
  readonly error: string;
  readonly path: string;
  readonly timestamp?: string;
  readonly fieldErrors: ApiErrorBody["fieldErrors"];

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = "ApiError";
    this.status = body.status;
    this.error = body.error;
    this.path = body.path;
    this.timestamp = body.timestamp;
    this.fieldErrors = body.fieldErrors;
  }
}

export function getApiBaseUrl(): string {
  const base = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
  return `${base.replace(/\/$/, "")}/api`;
}

export function parseApiErrorBody(
  body: unknown,
  status: number,
  fallbackPath: string,
): ApiError {
  if (isApiErrorBody(body)) {
    return new ApiError({
      ...body,
      status: body.status ?? status,
      path: body.path ?? fallbackPath,
    });
  }

  return new ApiError({
    status,
    error: status >= 500 ? "Internal Server Error" : "Request Failed",
    message: "An unexpected error occurred",
    path: fallbackPath,
  });
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const candidate = value as Record<string, unknown>;
  return (
    typeof candidate.status === "number" &&
    typeof candidate.error === "string" &&
    typeof candidate.message === "string" &&
    typeof candidate.path === "string"
  );
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const url = `${getApiBaseUrl()}${path}`;
  const headers = new Headers(options.headers);

  if (options.body !== undefined && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let body: unknown;
    try {
      body = await response.json();
    } catch {
      body = undefined;
    }
    throw parseApiErrorBody(body, response.status, path);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
