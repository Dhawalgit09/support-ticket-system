"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ErrorMessage, getDisplayError } from "@/components/ErrorMessage";
import Spinner from "@/components/ui/Spinner";
import { ApiError } from "@/lib/api/client";
import { createTicket } from "@/lib/api/tickets";
import { formatTicketPriority } from "@/lib/format";
import type { TicketPriority } from "@/lib/types/ticket";

const PRIORITY_OPTIONS: TicketPriority[] = ["LOW", "MEDIUM", "HIGH"];

const CREATE_ERROR_MESSAGE =
  "Unable to create ticket. Please try again.";

export default function TicketForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<TicketPriority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [bannerError, setBannerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const validateClient = (): Record<string, string> => {
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
  };

  const applyApiFieldErrors = (error: unknown) => {
    const display = getDisplayError(error);
    setBannerError(display.message);

    if (display.fieldErrors?.length) {
      const nextErrors: Record<string, string> = {};
      for (const fieldError of display.fieldErrors) {
        nextErrors[fieldError.field] = fieldError.message;
      }
      setFieldErrors(nextErrors);
    }
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBannerError(null);

    const clientErrors = validateClient();
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      setBannerError("One or more fields are invalid");
      return;
    }

    setFieldErrors({});
    setSubmitting(true);

    try {
      const created = await createTicket({
        title: title.trim(),
        description: description.trim(),
        priority,
        assignee: assignee.trim() || undefined,
      });
      router.push(`/tickets/${created.id}`);
    } catch (error) {
      if (error instanceof ApiError && error.status >= 500) {
        setBannerError(CREATE_ERROR_MESSAGE);
      } else if (error instanceof TypeError) {
        setBannerError(CREATE_ERROR_MESSAGE);
      } else {
        applyApiFieldErrors(error);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="ticket-form" onSubmit={handleSubmit} noValidate>
      {bannerError && <ErrorMessage message={bannerError} />}

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
          onChange={(event) => setTitle(event.target.value)}
          maxLength={200}
          disabled={submitting}
          placeholder="Brief summary of the issue"
        />
        <span className="ticket-form__hint">{title.length}/200</span>
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
          onChange={(event) => setDescription(event.target.value)}
          maxLength={5000}
          disabled={submitting}
          placeholder="Describe the issue in detail…"
        />
        <span className="ticket-form__hint">{description.length}/5000</span>
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
              onClick={() => setPriority(option)}
              disabled={submitting}
              aria-pressed={priority === option}
            >
              {formatTicketPriority(option)}
            </button>
          ))}
        </div>
        {fieldErrors.priority && (
          <p className="ticket-form__error" role="alert">{fieldErrors.priority}</p>
        )}
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
          onChange={(event) => setAssignee(event.target.value)}
          maxLength={100}
          disabled={submitting}
          placeholder="Optional — email or name"
        />
        {fieldErrors.assignee && (
          <p className="ticket-form__error" role="alert">{fieldErrors.assignee}</p>
        )}
      </div>

      <div className="ticket-form__actions">
        <button type="submit" className="btn btn--primary" disabled={submitting}>
          {submitting ? (
            <>
              <Spinner size="sm" />
              Creating…
            </>
          ) : (
            "Create Ticket"
          )}
        </button>
        <Link href="/" className="ticket-form__cancel">
          Cancel
        </Link>
      </div>
    </form>
  );
}
