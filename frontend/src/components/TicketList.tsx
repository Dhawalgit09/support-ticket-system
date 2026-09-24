"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { ErrorMessage, getDisplayError } from "@/components/ErrorMessage";
import Spinner from "@/components/ui/Spinner";
import TicketCard from "@/components/ui/TicketCard";
import TicketCardSkeleton from "@/components/ui/TicketCardSkeleton";
import { listTickets, type ListTicketsFilters } from "@/lib/api/tickets";
import { formatTicketStatus } from "@/lib/format";
import { useDebounce } from "@/lib/useDebounce";
import type { TicketStatus, TicketSummary } from "@/lib/types/ticket";

const STATUS_OPTIONS: Array<{ value: "" | TicketStatus; label: string }> = [
  { value: "", label: "All" },
  { value: "OPEN", label: "Open" },
  { value: "IN_PROGRESS", label: "In Progress" },
  { value: "RESOLVED", label: "Resolved" },
  { value: "CLOSED", label: "Closed" },
  { value: "CANCELLED", label: "Cancelled" },
];

export default function TicketList() {
  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ReturnType<typeof getDisplayError> | null>(
    null,
  );
  const [searchInput, setSearchInput] = useState("");
  const [statusInput, setStatusInput] = useState<"" | TicketStatus>("");
  const debouncedSearch = useDebounce(searchInput.trim(), 350);

  const fetchTickets = useCallback(async (filters: ListTicketsFilters) => {
    setLoading(true);
    setError(null);

    try {
      const data = await listTickets(filters);
      setTickets(data);
    } catch (err) {
      setError(getDisplayError(err));
      setTickets([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchTickets({
      search: debouncedSearch || undefined,
      status: statusInput || undefined,
    });
  }, [debouncedSearch, statusInput, fetchTickets]);

  const stats = useMemo(() => {
    const open = tickets.filter((t) => t.status === "OPEN").length;
    const inProgress = tickets.filter((t) => t.status === "IN_PROGRESS").length;
    const resolved = tickets.filter((t) => t.status === "RESOLVED").length;
    return { total: tickets.length, open, inProgress, resolved };
  }, [tickets]);

  const hasActiveFilters = Boolean(debouncedSearch || statusInput);
  const showStats = !loading && !hasActiveFilters && tickets.length > 0;

  return (
    <div className="ticket-list">
      <div className="ticket-list__hero">
        <div>
          <h1 className="ticket-list__title">Tickets</h1>
          <p className="ticket-list__subtitle">
            Search, filter, and manage support requests
          </p>
        </div>
        <Link href="/tickets/new" className="app-header__action">
          + New Ticket
        </Link>
      </div>

      {showStats && (
        <div className="ticket-list__stats">
          <div className="ticket-list__stat">
            <span className="ticket-list__stat-value">{stats.total}</span>
            <span className="ticket-list__stat-label">Total</span>
          </div>
          <div className="ticket-list__stat">
            <span className="ticket-list__stat-value">{stats.open}</span>
            <span className="ticket-list__stat-label">Open</span>
          </div>
          <div className="ticket-list__stat">
            <span className="ticket-list__stat-value">{stats.inProgress}</span>
            <span className="ticket-list__stat-label">In Progress</span>
          </div>
          <div className="ticket-list__stat">
            <span className="ticket-list__stat-value">{stats.resolved}</span>
            <span className="ticket-list__stat-label">Resolved</span>
          </div>
        </div>
      )}

      <div className="ticket-list__filters">
        <div className="ticket-list__search-wrap">
          <span className="ticket-list__search-icon" aria-hidden="true">⌕</span>
          <input
            type="search"
            className="ticket-list__input"
            placeholder="Search title or description…"
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            aria-label="Search tickets"
          />
        </div>

        <div className="ticket-list__status-pills" role="group" aria-label="Filter by status">
          {STATUS_OPTIONS.map((option) => (
            <button
              key={option.label}
              type="button"
              className={`ticket-list__pill${statusInput === option.value ? " ticket-list__pill--active" : ""}`}
              onClick={() => setStatusInput(option.value)}
            >
              {option.value ? formatTicketStatus(option.value) : option.label}
            </button>
          ))}
        </div>
      </div>

      {error && <ErrorMessage {...error} />}

      {loading ? (
        <div className="ticket-list__grid" aria-live="polite" aria-busy="true">
          {Array.from({ length: 6 }).map((_, index) => (
            <TicketCardSkeleton key={index} />
          ))}
        </div>
      ) : tickets.length === 0 ? (
        <div className="ticket-list__empty">
          <span className="ticket-list__empty-icon" aria-hidden="true">📋</span>
          {hasActiveFilters ? (
            <p>No tickets match your filters. Try adjusting your search.</p>
          ) : (
            <>
              <p>No tickets yet. Create your first one to get started.</p>
              <Link href="/tickets/new" className="ticket-list__empty-link">
                Create Ticket
              </Link>
            </>
          )}
        </div>
      ) : (
        <div className="ticket-list__grid">
          {tickets.map((ticket, index) => (
            <div key={ticket.id} style={{ animationDelay: `${index * 40}ms` }}>
              <TicketCard ticket={ticket} />
            </div>
          ))}
        </div>
      )}

      {loading && tickets.length > 0 && (
        <p className="ticket-list__status">
          <Spinner size="sm" />
          Refreshing…
        </p>
      )}
    </div>
  );
}
