import Link from "next/link";
import {
  formatDateTime,
  formatRelativeTime,
  formatTicketPriority,
  formatTicketStatus,
} from "@/lib/format";
import type { TicketSummary } from "@/lib/types/ticket";

interface TicketCardProps {
  ticket: TicketSummary;
}

export default function TicketCard({ ticket }: TicketCardProps) {
  return (
    <Link href={`/tickets/${ticket.id}`} className="ticket-card">
      <div className="ticket-card__header">
        <span className="ticket-card__id">#{ticket.id}</span>
        <span
          className={`status-badge status-badge--${ticket.status.toLowerCase()}`}
        >
          {formatTicketStatus(ticket.status)}
        </span>
      </div>

      <h3 className="ticket-card__title">{ticket.title}</h3>

      <div className="ticket-card__footer">
        <span
          className={`priority-badge priority-badge--${ticket.priority.toLowerCase()}`}
        >
          {formatTicketPriority(ticket.priority)}
        </span>
        <span className="ticket-card__assignee">
          {ticket.assignee ?? "Unassigned"}
        </span>
        <time
          className="ticket-card__time"
          dateTime={ticket.updatedAt}
          title={formatDateTime(ticket.updatedAt)}
        >
          {formatRelativeTime(ticket.updatedAt)}
        </time>
      </div>
    </Link>
  );
}
