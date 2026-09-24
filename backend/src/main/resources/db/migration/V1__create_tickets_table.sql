CREATE TABLE tickets (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title           VARCHAR(200)  NOT NULL,
    description     VARCHAR(5000) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    priority        VARCHAR(10)   NOT NULL DEFAULT 'MEDIUM',
    assignee        VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_tickets_status CHECK (
        status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED')
    ),
    CONSTRAINT chk_tickets_priority CHECK (
        priority IN ('LOW', 'MEDIUM', 'HIGH')
    )
);

CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_updated_at ON tickets (updated_at DESC);
