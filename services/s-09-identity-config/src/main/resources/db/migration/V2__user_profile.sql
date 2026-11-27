-- US-021: people who have signed in. Identity and credentials live in Keycloak (ADR-050).
CREATE TABLE user_profile (
    subject       VARCHAR(64)  PRIMARY KEY,
    username      VARCHAR(255) NOT NULL,
    email         VARCHAR(320),
    display_name  VARCHAR(255),
    roles         VARCHAR(255) NOT NULL,
    first_seen_at TIMESTAMPTZ  NOT NULL,
    last_seen_at  TIMESTAMPTZ  NOT NULL
);
