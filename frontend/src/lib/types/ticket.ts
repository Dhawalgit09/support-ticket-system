export type TicketStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "CANCELLED";

export type TicketPriority = "LOW" | "MEDIUM" | "HIGH";

export interface TicketSummary {
  id: number;
  title: string;
  status: TicketStatus;
  priority: TicketPriority;
  assignee: string | null;
  updatedAt: string;
}

export interface Comment {
  id: number;
  body: string;
  author: string | null;
  createdAt: string;
}

export interface TicketDetail {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
  comments: Comment[];
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority?: TicketPriority;
  assignee?: string | null;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string;
  priority?: TicketPriority;
  assignee?: string | null;
}

export interface UpdateTicketStatusRequest {
  status: TicketStatus;
}

export interface CreateCommentRequest {
  body: string;
  author?: string | null;
}

export interface FieldError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

export interface ApiErrorBody {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: FieldError[];
}
