import { formatTicketStatus } from "@/lib/format";
import type { TicketStatus } from "@/lib/types/ticket";

const PIPELINE: TicketStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "RESOLVED",
  "CLOSED",
];

interface StatusPipelineProps {
  status: TicketStatus;
}

function stepState(
  step: TicketStatus,
  current: TicketStatus,
): "complete" | "current" | "upcoming" | "cancelled" {
  if (current === "CANCELLED") {
    return step === "OPEN" ? "cancelled" : "upcoming";
  }

  const currentIndex = PIPELINE.indexOf(current);
  const stepIndex = PIPELINE.indexOf(step);

  if (stepIndex < currentIndex) {
    return "complete";
  }
  if (stepIndex === currentIndex) {
    return "current";
  }
  return "upcoming";
}

export default function StatusPipeline({ status }: StatusPipelineProps) {
  if (status === "CANCELLED") {
    return (
      <div className="status-pipeline status-pipeline--cancelled">
        <span className="status-pipeline__cancelled-badge">Cancelled</span>
        <p className="status-pipeline__cancelled-note">
          This ticket was cancelled and will not progress further.
        </p>
      </div>
    );
  }

  return (
    <ol className="status-pipeline" aria-label="Ticket status progress">
      {PIPELINE.map((step, index) => {
        const state = stepState(step, status);
        return (
          <li
            key={step}
            className={`status-pipeline__step status-pipeline__step--${state}`}
          >
            <span className="status-pipeline__dot" aria-hidden="true">
              {state === "complete" ? "✓" : index + 1}
            </span>
            <span className="status-pipeline__label">
              {formatTicketStatus(step)}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
