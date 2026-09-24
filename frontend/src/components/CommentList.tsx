import { formatDateTime, formatRelativeTime } from "@/lib/format";
import type { Comment } from "@/lib/types/ticket";

interface CommentListProps {
  comments: Comment[];
}

function authorInitials(author: string | null): string {
  const name = author?.trim() || "A";
  const parts = name.split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return name.slice(0, 2).toUpperCase();
}

export default function CommentList({ comments }: CommentListProps) {
  if (comments.length === 0) {
    return (
      <p className="comment-list__empty">
        No comments yet. Be the first to add one below.
      </p>
    );
  }

  return (
    <ol className="comment-list">
      {comments.map((comment, index) => (
        <li
          key={comment.id}
          className="comment-list__item"
          style={{ animationDelay: `${index * 50}ms` }}
        >
          <span className="comment-list__avatar" aria-hidden="true">
            {authorInitials(comment.author)}
          </span>
          <div className="comment-list__content">
            <p className="comment-list__body">{comment.body}</p>
            <p className="comment-list__meta">
              <strong>{comment.author ?? "Anonymous"}</strong>
              {" · "}
              <time dateTime={comment.createdAt} title={formatDateTime(comment.createdAt)}>
                {formatRelativeTime(comment.createdAt)}
              </time>
            </p>
          </div>
        </li>
      ))}
    </ol>
  );
}
