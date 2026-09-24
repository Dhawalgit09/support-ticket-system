import TicketForm from "@/components/TicketForm";

export default function NewTicketPage() {
  return (
    <div className="ticket-page">
      <div className="ticket-page__header">
        <h1 className="ticket-page__title">Create Ticket</h1>
        <p className="ticket-page__subtitle">
          New tickets are created with status Open.
        </p>
      </div>
      <TicketForm />
    </div>
  );
}
