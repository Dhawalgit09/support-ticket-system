import { describe, expect, it } from "vitest";
import { ApiError, parseApiErrorBody } from "@/lib/api/client";

describe("parseApiErrorBody", () => {
  it("parsesValidationError_withFieldErrors", () => {
    const error = parseApiErrorBody(
      {
        timestamp: "2026-09-24T14:30:00Z",
        status: 400,
        error: "Validation Failed",
        message: "One or more fields are invalid",
        path: "/api/tickets",
        fieldErrors: [
          {
            field: "title",
            message: "must not be blank",
            rejectedValue: "",
          },
        ],
      },
      400,
      "/tickets",
    );

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(400);
    expect(error.error).toBe("Validation Failed");
    expect(error.message).toBe("One or more fields are invalid");
    expect(error.path).toBe("/api/tickets");
    expect(error.fieldErrors).toHaveLength(1);
    expect(error.fieldErrors?.[0].field).toBe("title");
  });

  it("parsesNotFoundError_withoutFieldErrors", () => {
    const error = parseApiErrorBody(
      {
        timestamp: "2026-09-24T14:30:00Z",
        status: 404,
        error: "Not Found",
        message: "Ticket not found with id: 42",
        path: "/api/tickets/42",
      },
      404,
      "/tickets/42",
    );

    expect(error.status).toBe(404);
    expect(error.error).toBe("Not Found");
    expect(error.fieldErrors).toBeUndefined();
  });

  it("parsesConflictError_forInvalidStatusTransition", () => {
    const error = parseApiErrorBody(
      {
        status: 409,
        error: "Invalid Status Transition",
        message: "Cannot transition from CLOSED to OPEN",
        path: "/api/tickets/1/status",
      },
      409,
      "/tickets/1/status",
    );

    expect(error.status).toBe(409);
    expect(error.error).toBe("Invalid Status Transition");
    expect(error.message).toContain("CLOSED");
  });

  it("parsesServerError_withGenericFallback", () => {
    const error = parseApiErrorBody(
      {
        status: 500,
        error: "Internal Server Error",
        message: "An unexpected error occurred",
        path: "/api/tickets",
      },
      500,
      "/tickets",
    );

    expect(error.status).toBe(500);
    expect(error.error).toBe("Internal Server Error");
  });

  it("usesFallback_whenBodyIsNotApiErrorShape", () => {
    const error = parseApiErrorBody({ unexpected: true }, 400, "/tickets");

    expect(error.status).toBe(400);
    expect(error.error).toBe("Request Failed");
    expect(error.message).toBe("An unexpected error occurred");
    expect(error.path).toBe("/tickets");
  });
});
