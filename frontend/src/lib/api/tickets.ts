import { apiRequest } from "@/lib/api/client";
import type {
  CreateTicketRequest,
  TicketDetail,
  TicketStatus,
  TicketSummary,
  UpdateTicketRequest,
  UpdateTicketStatusRequest,
} from "@/lib/types/ticket";

export interface ListTicketsFilters {
  search?: string;
  status?: TicketStatus;
}

export async function listTickets(
  filters?: ListTicketsFilters,
): Promise<TicketSummary[]> {
  const params = new URLSearchParams();

  if (filters?.search?.trim()) {
    params.set("search", filters.search.trim());
  }
  if (filters?.status) {
    params.set("status", filters.status);
  }

  const query = params.toString();
  const path = query ? `/tickets?${query}` : "/tickets";
  return apiRequest<TicketSummary[]>(path);
}

export async function getTicket(id: number): Promise<TicketDetail> {
  return apiRequest<TicketDetail>(`/tickets/${id}`);
}

export async function createTicket(
  request: CreateTicketRequest,
): Promise<TicketDetail> {
  return apiRequest<TicketDetail>("/tickets", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export async function updateTicket(
  id: number,
  request: UpdateTicketRequest,
): Promise<TicketDetail> {
  return apiRequest<TicketDetail>(`/tickets/${id}`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export async function updateTicketStatus(
  id: number,
  request: UpdateTicketStatusRequest,
): Promise<TicketDetail> {
  return apiRequest<TicketDetail>(`/tickets/${id}/status`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}
