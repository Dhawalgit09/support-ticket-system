export default function TicketCardSkeleton() {
  return (
    <div className="ticket-card ticket-card--skeleton" aria-hidden="true">
      <div className="ticket-card__header">
        <span className="skeleton skeleton--text skeleton--sm" />
        <span className="skeleton skeleton--badge" />
      </div>
      <span className="skeleton skeleton--text skeleton--lg" />
      <div className="ticket-card__footer">
        <span className="skeleton skeleton--badge" />
        <span className="skeleton skeleton--text skeleton--md" />
      </div>
    </div>
  );
}
