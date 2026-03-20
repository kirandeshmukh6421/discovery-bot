CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE discovery_entries ADD COLUMN embedding vector(1536);

CREATE INDEX ON discovery_entries USING hnsw (embedding vector_cosine_ops);
