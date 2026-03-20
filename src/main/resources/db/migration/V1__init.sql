CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    name       VARCHAR(255),
    username   VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS groups (
    id                BIGSERIAL PRIMARY KEY,
    telegram_group_id BIGINT UNIQUE NOT NULL,
    name              VARCHAR(255),
    created_at        TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_groups (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    group_id   BIGINT NOT NULL REFERENCES groups(id),
    role       VARCHAR(50),
    joined_at  TIMESTAMP,
    UNIQUE (user_id, group_id)
);

CREATE TABLE IF NOT EXISTS discovery_entries (
    id             BIGSERIAL PRIMARY KEY,
    group_id       BIGINT NOT NULL REFERENCES groups(id),
    added_by       BIGINT NOT NULL REFERENCES users(id),
    raw_input      TEXT,
    user_note      TEXT,
    extracted_data JSONB,
    category       VARCHAR(255),
    source         VARCHAR(100),
    tags           VARCHAR(1000),
    created_at     TIMESTAMP NOT NULL
);
