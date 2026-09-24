"use client";

import { useState } from "react";
import { ErrorMessage, getDisplayError } from "@/components/ErrorMessage";
import ConfirmDialog from "@/components/ui/ConfirmDialog";
import Spinner from "@/components/ui/Spinner";
import { ApiError } from "@/lib/api/client";
import { updateTicketStatus } from "@/lib/api/tickets";
import type { TicketDetail, TicketStatus } from "@/lib/types/ticket";

type TransitionAction = {
  label: string;
  target: TicketStatus;
  destructive?: boolean;
};

const TRANSITIONS: Record<TicketStatus, TransitionAction[]> = {
  OPEN: [
    { label: "Start Progress", target: "IN_PROGRESS" },
    { label: "Cancel Ticket", target: "CANCELLED", destructive: true },
  ],
  IN_PROGRESS: [
    { label: "Mark Resolved", target: "RESOLVED" },
    { label: "Cancel Ticket", target: "CANCELLED", destructive: true },
  ],
  RESOLVED: [{ label: "Close Ticket", target: "CLOSED" }],
  CLOSED: [],
  CANCELLED: [],
};

interface StatusTransitionProps {
  ticket: TicketDetail;
  onUpdated: (ticket: TicketDetail) => void;
}

export default function StatusTransition({
  ticket,
  onUpdated,
}: StatusTransitionProps) {
  const [transitioning, setTransitioning] = useState<TicketStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<TransitionAction | null>(
    null,
  );

  const actions = TRANSITIONS[ticket.status];

  const executeTransition = async (target: TicketStatus) => {
    setError(null);
    setTransitioning(target);

    try {
      const updated = await updateTicketStatus(ticket.id, { status: target });
      onUpdated(updated);
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setError(err.message);
      } else {
        setError(getDisplayError(err).message);
      }
    } finally {
      setTransitioning(null);
      setPendingAction(null);
    }
  };

  const handleActionClick = (action: TransitionAction) => {
    if (action.destructive) {
      setPendingAction(action);
      return;
    }
    void executeTransition(action.target);
  };

  return (
    <div className="ticket-detail__panel">
      <h2 className="ticket-detail__section-title">Actions</h2>

      {error && <ErrorMessage message={error} />}

      {ticket.status === "CLOSED" && (
        <p className="status-transition__message">This ticket is closed.</p>
      )}

      {ticket.status === "CANCELLED" && (
        <p className="status-transition__message">This ticket was cancelled.</p>
      )}

      {actions.length > 0 && (
        <div className="status-transition__actions">
          {actions.map((action) => (
            <button
              key={action.target}
              type="button"
              className={
                action.destructive
                  ? "status-transition__button status-transition__button--danger"
                  : "status-transition__button"
              }
              disabled={transitioning !== null}
              onClick={() => handleActionClick(action)}
            >
              {transitioning === action.target ? (
                <>
                  <Spinner size="sm" />
                  Updating…
                </>
              ) : (
                action.label
              )}
            </button>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={pendingAction !== null}
        title="Cancel ticket?"
        message="This ticket will be marked as cancelled and cannot be reopened."
        confirmLabel="Yes, cancel"
        destructive
        loading={transitioning === "CANCELLED"}
        onConfirm={() => {
          if (pendingAction) {
            void executeTransition(pendingAction.target);
          }
        }}
        onCancel={() => setPendingAction(null)}
      />
    </div>
  );
}
