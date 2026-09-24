import TicketDetailView from "@/components/TicketDetail";

type TicketDetailPageProps = {
  params: Promise<{ id: string }>;
};

export default async function TicketDetailPage({ params }: TicketDetailPageProps) {
  const { id } = await params;
  const ticketId = Number(id);

  return <TicketDetailView ticketId={ticketId} />;
}
