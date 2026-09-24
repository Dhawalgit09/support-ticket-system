import { apiRequest } from "@/lib/api/client";
import type { Comment, CreateCommentRequest } from "@/lib/types/ticket";

export async function addComment(
  ticketId: number,
  request: CreateCommentRequest,
): Promise<Comment> {
  return apiRequest<Comment>(`/tickets/${ticketId}/comments`, {
    method: "POST",
    body: JSON.stringify(request),
  });
}
