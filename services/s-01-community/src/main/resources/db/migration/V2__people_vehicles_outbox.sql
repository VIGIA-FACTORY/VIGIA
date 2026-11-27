-- US-031 and US-041 (MVP subset): community members, their vehicles and the outbox (ADR-006).
CREATE TABLE person (
    id              UUID         PRIMARY KEY,
    document_number VARCHAR(20)  NOT NULL UNIQUE,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    email           VARCHAR(320),
    member_type     VARCHAR(20)  NOT NULL,
    registered_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE vehicle (
    id            UUID        PRIMARY KEY,
    plate         VARCHAR(8)  NOT NULL UNIQUE,
    owner_id      UUID        NOT NULL REFERENCES person (id),
    brand         VARCHAR(60),
    model         VARCHAR(60),
    color         VARCHAR(30),
    registered_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE outbox_event (
    id           UUID         PRIMARY KEY,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    topic        VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX outbox_event_pending ON outbox_event (created_at) WHERE published_at IS NULL;
