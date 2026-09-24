CREATE TABLE comments (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ticket_id   BIGINT        NOT NULL,
    body        VARCHAR(2000) NOT NULL,
    author      VARCHAR(100),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comments_ticket
        FOREIGN KEY (ticket_id) REFERENCES tickets (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_comments_ticket_id ON comments (ticket_id);
