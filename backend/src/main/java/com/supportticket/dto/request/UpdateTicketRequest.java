package com.supportticket.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.supportticket.entity.TicketPriority;
import com.supportticket.entity.TicketStatus;
import com.supportticket.validation.AtLeastOneFieldPresent;
import jakarta.validation.constraints.Size;

@AtLeastOneFieldPresent
public class UpdateTicketRequest {

    @Size(min = 1, max = 200)
    private String title;

    @Size(min = 1, max = 5000)
    private String description;

    private TicketPriority priority;

    @Size(max = 100)
    private String assignee;

    private TicketStatus status;

    @JsonIgnore
    private boolean assigneeIncluded;

    public UpdateTicketRequest() {
    }

    public UpdateTicketRequest(
            String title,
            String description,
            TicketPriority priority,
            String assignee,
            TicketStatus status) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignee = assignee;
        this.status = status;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketPriority priority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public String assignee() {
        return assignee;
    }

    @JsonProperty("assignee")
    public void setAssignee(String assignee) {
        this.assigneeIncluded = true;
        this.assignee = assignee;
    }

    public TicketStatus status() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public boolean assigneeIncluded() {
        return assigneeIncluded;
    }
}
