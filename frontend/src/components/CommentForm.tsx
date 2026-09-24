"use client";

import { useState } from "react";
import { ErrorMessage, getDisplayError } from "@/components/ErrorMessage";
import { ApiError } from "@/lib/api/client";
import { addComment } from "@/lib/api/comments";
import { getTicket } from "@/lib/api/tickets";
import type { TicketDetail } from "@/lib/types/ticket";

const COMMENT_ERROR_MESSAGE =
  "Unable to add comment. Please try again.";

interface CommentFormProps {
  ticketId: number;
  onSuccess: (ticket: TicketDetail) => void;
}

export default function CommentForm({ ticketId, onSuccess }: CommentFormProps) {
  const [body, setBody] = useState("");
  const [author, setAuthor] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [bannerError, setBannerError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBannerError(null);

    const trimmedBody = body.trim();
    const trimmedAuthor = author.trim();

    if (!trimmedBody) {
      setFieldErrors({ body: "Comment body is required" });
      setBannerError("One or more fields are invalid");
      return;
    }

    if (trimmedBody.length > 2000) {
      setFieldErrors({ body: "Comment must be at most 2000 characters" });
      setBannerError("One or more fields are invalid");
      return;
    }

    if (trimmedAuthor.length > 100) {
      setFieldErrors({ author: "Author must be at most 100 characters" });
      setBannerError("One or more fields are invalid");
      return;
    }

    setFieldErrors({});
    setSubmitting(true);

    try {
      await addComment(ticketId, {
        body: trimmedBody,
        author: trimmedAuthor || undefined,
      });

      const updated = await getTicket(ticketId);
      setBody("");
      onSuccess(updated);
    } catch (error) {
      if (error instanceof ApiError && error.status >= 500) {
        setBannerError(COMMENT_ERROR_MESSAGE);
      } else if (error instanceof TypeError) {
        setBannerError(COMMENT_ERROR_MESSAGE);
      } else if (error instanceof ApiError && error.fieldErrors?.length) {
        const nextErrors: Record<string, string> = {};
        for (const fieldError of error.fieldErrors) {
          nextErrors[fieldError.field] = fieldError.message;
        }
        setFieldErrors(nextErrors);
        setBannerError(error.message);
      } else {
        setBannerError(getDisplayError(error).message);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="comment-form" onSubmit={handleSubmit} noValidate>
      {bannerError && <ErrorMessage message={bannerError} />}

      <div className="ticket-form__field">
        <label className="ticket-form__label" htmlFor="comment-body">
          Comment <span className="ticket-form__required">*</span>
        </label>
        <textarea
          id="comment-body"
          name="body"
          className="ticket-form__textarea"
          rows={4}
          value={body}
          onChange={(event) => setBody(event.target.value)}
          maxLength={2000}
          disabled={submitting}
        />
        {fieldErrors.body && (
          <p className="ticket-form__error" role="alert">{fieldErrors.body}</p>
        )}
      </div>

      <div className="ticket-form__field">
        <label className="ticket-form__label" htmlFor="comment-author">
          Author
        </label>
        <input
          id="comment-author"
          name="author"
          type="text"
          className="ticket-form__input"
          value={author}
          onChange={(event) => setAuthor(event.target.value)}
          maxLength={100}
          disabled={submitting}
        />
        {fieldErrors.author && (
          <p className="ticket-form__error" role="alert">{fieldErrors.author}</p>
        )}
      </div>

      <div className="ticket-form__actions">
        <button type="submit" className="btn btn--primary" disabled={submitting}>
          {submitting ? "Adding…" : "Add Comment"}
        </button>
      </div>
    </form>
  );
}
