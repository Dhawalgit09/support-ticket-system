"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ErrorMessage, getDisplayError } from "@/components/ErrorMessage";
import CommentForm from "@/components/CommentForm";
import CommentList from "@/components/CommentList";
import StatusTransition from "@/components/StatusTransition";
import Spinner from "@/components/ui/Spinner";
import StatusPipeline from "@/components/ui/StatusPipeline";
import Toast from "@/components/ui/Toast";
import { ApiError } from "@/lib/api/client";
import { getTicket, updateTicket } from "@/lib/api/tickets";
import {
  formatDateTime,
  formatTicketPriority,
  formatTicketStatus,
} from "@/lib/format";
import type { TicketDetail, TicketPriority } from "@/lib/types/ticket";

const PRIORITY_OPTIONS: TicketPriority[] = ["LOW", "MEDIUM", "HIGH"];

const SAVE_ERROR_MESSAGE =
  "Unable to save changes. Please try again.";

interface TicketDetailProps {
  ticketId: number;
}

function validateFields(
  title: string,
  description: string,
  assignee: string,
): Record<string, string> {
  const errors: Record<string, string> = {};
  const trimmedTitle = title.trim();
  const trimmedDescription = description.trim();
  const trimmedAssignee = assignee.trim();

  if (!trimmedTitle) {
    errors.title = "Title is required";
  } else if (trimmedTitle.length > 200) {
    errors.title = "Title must be at most 200 characters";
  }

  if (!trimmedDescription) {
    errors.description = "Description is required";
  } else if (trimmedDescription.length > 5000) {
    errors.description = "Description must be at most 5000 characters";
  }

  if (trimmedAssignee.length > 100) {
    errors.assignee = "Assignee must be at most 100 characters";
  }

  return errors;
}

export default function TicketDetailView({ ticketId }: TicketDetailProps) {
  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TicketPriority>("MEDIUM");
  const [assignee, setAssignee] = useState("");

  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [saveError, setSaveError] = useState<string | null>(null);
  const [showSaveToast, setShowSaveToast] = useState(false);
  const [saving, setSaving] = useState(false);

  const syncForm = useCallback((detail: TicketDetail) => {
    setTitle(detail.title);
    setDescription(detail.description);
    setPriority(detail.priority);
    setAssignee(detail.assignee ?? "");
  }, []);

  const loadTicket = useCallback(async () => {
    if (Number.isNaN(ticketId)) {
      setNotFound(true);
      setLoading(false);
      return;
    }

    setLoading(true);
    setLoadError(null);
    setNotFound(false);

    try {
      const detail = await getTicket(ticketId);
      setTicket(detail);
      syncForm(detail);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setNotFound(true);
        setTicket(null);
      } else {
        setLoadError(getDisplayError(error).message);
      }
    } finally {
      setLoading(false);
    }
  }, [syncForm, ticketId]);

  useEffect(() => {
    void loadTicket();
  }, [loadTicket]);

  const isDirty = useMemo(() => {
    if (!ticket) {
      return false;
    }
    const normalizedAssignee = assignee.trim() || null;
    return (
      title.trim() !== ticket.title ||
      description.trim() !== ticket.description ||
      priority !== ticket.priority ||
      normalizedAssignee !== ticket.assignee
    );
  }, [assignee, description, priority, ticket, title]);

  const applyApiFieldErrors = (error: unknown) => {
    const display = getDisplayError(error);
    setSaveError(display.message);

    if (display.fieldErrors?.length) {
      const nextErrors: Record<string, string> = {};
      for (const fieldError of display.fieldErrors) {
        nextErrors[fieldError.field] = fieldError.message;
      }
      setFieldErrors(nextErrors);
    }
  };

  const handleSave = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!ticket) {
      return;
    }

    setSaveError(null);
    setShowSaveToast(false);

    const clientErrors = validateFields(title, description, assignee);
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      setSaveError("One or more fields are invalid");
      return;
    }

    const trimmedTitle = title.trim();
    const trimmedDescription = description.trim();
    const trimmedAssignee = assignee.trim();
    const normalizedAssignee = trimmedAssignee.length > 0 ? trimmedAssignee : null;

    const payload: {
      title?: string;
      description?: string;
      priority?: TicketPriority;
      assignee?: string | null;
    } = {};

    if (trimmedTitle !== ticket.title) {
      payload.title = trimmedTitle;
    }
    if (trimmedDescription !== ticket.description) {
      payload.description = trimmedDescription;
    }
    if (priority !== ticket.priority) {
      payload.priority = priority;
    }
    if (normalizedAssignee !== ticket.assignee) {
      payload.assignee = normalizedAssignee;
    }

    if (Object.keys(payload).length === 0) {
      setSaveError("No changes to save");
      return;
    }

    setFieldErrors({});
    setSaving(true);

    try {
      const updated = await updateTicket(ticket.id, payload);
      setTicket(updated);
      syncForm(updated);
      setShowSaveToast(true);
    } catch (error) {
      if (error instanceof ApiError && error.status >= 500) {
        setSaveError(SAVE_ERROR_MESSAGE);
      } else if (error instanceof TypeError) {
        setSaveError(SAVE_ERROR_MESSAGE);
      } else {
        applyApiFieldErrors(error);
      }
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <p className="ticket-detail__status">
        <Spinner />
        Loading ticket…
      </p>
    );
  }

  if (notFound) {
    return (
      <div className="ticket-detail__empty">
        <p>Ticket not found.</p>
        <Link href="/" className="ticket-detail__back-link">← Back to list</Link>
      </div>
    );
  }

  if (loadError) {
    return (
      <div className="ticket-detail">
        <ErrorMessage message={loadError} />
        <Link href="/" className="ticket-detail__back-link">← Back to list</Link>
      </div>
    );
  }

  if (!ticket) {
    return null;
  }

  return (
    <div className="ticket-detail">
      {showSaveToast && (
        <Toast
          message="Changes saved successfully"
          onDismiss={() => setShowSaveToast(false)}
        />
      )}

      <div className="ticket-detail__top">
        <Link href="/" className="ticket-detail__back-link">← Back to tickets</Link>
        <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", flexWrap: "wrap" }}>
          <h1 className="ticket-detail__heading">#{ticket.id}</h1>
          {isDirty && <span className="ticket-detail__dirty">Unsaved changes</span>}
        </div>
      </div>

      <div className="ticket-detail__layout">
        <div className="ticket-detail__main">
          <div className="ticket-detail__panel">
            <StatusPipeline status={ticket.status} />
          </div>

          <div className="ticket-detail__panel">
            <h2 className="ticket-detail__section-title">Details</h2>

            <form className="ticket-form" onSubmit={handleSave} noValidate style={{ boxShadow: "none", border: "none", padding: 0, maxWidth: "none" }}>
              {saveError && <ErrorMessage message={saveError} />}

              <div className="ticket-form__field">
                <label className="ticket-form__label" htmlFor="title">
                  Title <span className="ticket-form__required">*</span>
                </label>
                <input
                  id="title"
                  name="title"
                  type="text"
                  className="ticket-form__input"
                  value={title}
                  onChange={(event) => {
                    setShowSaveToast(false);
                    setTitle(event.target.value);
                  }}
                  maxLength={200}
                  disabled={saving}
                />
                {fieldErrors.title && (
                  <p className="ticket-form__error" role="alert">{fieldErrors.title}</p>
                )}
              </div>

              <div className="ticket-form__field">
                <label className="ticket-form__label" htmlFor="description">
                  Description <span className="ticket-form__required">*</span>
                </label>
                <textarea
                  id="description"
                  name="description"
                  className="ticket-form__textarea"
                  rows={6}
                  value={description}
                  onChange={(event) => {
                    setShowSaveToast(false);
                    setDescription(event.target.value);
                  }}
                  maxLength={5000}
                  disabled={saving}
                />
                {fieldErrors.description && (
                  <p className="ticket-form__error" role="alert">
                    {fieldErrors.description}
                  </p>
                )}
              </div>

              <div className="ticket-form__field">
                <span className="ticket-form__label">Priority</span>
                <div className="priority-picker" role="group" aria-label="Priority">
                  {PRIORITY_OPTIONS.map((option) => (
                    <button
                      key={option}
                      type="button"
                      className={`priority-picker__option priority-picker__option--${option.toLowerCase()}${priority === option ? " priority-picker__option--selected" : ""}`}
                      onClick={() => {
                        setShowSaveToast(false);
                        setPriority(option);
                      }}
                      disabled={saving}
                      aria-pressed={priority === option}
                    >
                      {formatTicketPriority(option)}
                    </button>
                  ))}
                </div>
              </div>

              <div className="ticket-form__field">
                <label className="ticket-form__label" htmlFor="assignee">
                  Assignee
                </label>
                <input
                  id="assignee"
                  name="assignee"
                  type="text"
                  className="ticket-form__input"
                  value={assignee}
                  onChange={(event) => {
                    setShowSaveToast(false);
                    setAssignee(event.target.value);
                  }}
                  maxLength={100}
                  disabled={saving}
                  placeholder="Unassigned"
                />
                {fieldErrors.assignee && (
                  <p className="ticket-form__error" role="alert">
                    {fieldErrors.assignee}
                  </p>
                )}
              </div>

              <div className="ticket-form__actions">
                <button
                  type="submit"
                  className="btn btn--primary"
                  disabled={saving || !isDirty}
                >
                  {saving ? (
                    <>
                      <Spinner size="sm" />
                      Saving…
                    </>
                  ) : (
                    "Save changes"
                  )}
                </button>
              </div>
            </form>
          </div>

          <div className="ticket-detail__panel">
            <h2 className="ticket-detail__section-title">Comments</h2>
            <CommentList comments={ticket.comments} />
            <CommentForm
              ticketId={ticket.id}
              onSuccess={(updated) => {
                setTicket(updated);
                setShowSaveToast(false);
              }}
            />
          </div>
        </div>

        <aside className="ticket-detail__sidebar">
          <div className="ticket-detail__panel">
            <h2 className="ticket-detail__section-title">Overview</h2>
            <div className="ticket-detail__meta" style={{ flexDirection: "column", alignItems: "flex-start", gap: "0.625rem" }}>
              <span
                className={`status-badge status-badge--${ticket.status.toLowerCase()}`}
              >
                {formatTicketStatus(ticket.status)}
              </span>
              <span
                className={`priority-badge priority-badge--${ticket.priority.toLowerCase()}`}
              >
                {formatTicketPriority(ticket.priority)}
              </span>
              <span className="ticket-detail__meta-item">
                Created {formatDateTime(ticket.createdAt)}
              </span>
              <span className="ticket-detail__meta-item">
                Updated {formatDateTime(ticket.updatedAt)}
              </span>
              <span className="ticket-detail__meta-item">
                {ticket.comments.length} comment{ticket.comments.length !== 1 ? "s" : ""}
              </span>
            </div>
          </div>

          <StatusTransition
            ticket={ticket}
            onUpdated={(updated) => {
              setTicket(updated);
              syncForm(updated);
              setShowSaveToast(false);
            }}
          />
        </aside>
      </div>
    </div>
  );
}
