CREATE TABLE public.connection_records(
  "id" VARCHAR(36) NOT NULL PRIMARY KEY,
  "thid" VARCHAR(36) NOT NULL,
  "role"  VARCHAR(50) NOT NULL,
  "protocol_state" VARCHAR(50) NOT NULL,
  "invitation" TEXT,
  "connection_request" TEXT,
  "connection_response" TEXT
);