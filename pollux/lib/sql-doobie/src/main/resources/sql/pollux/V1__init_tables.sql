-- CREATE TYPE public.operation_type AS ENUM(
--   'CREATE',
--   'UPDATE',
--   'RECOVER',
--   'DEACTIVATE'
-- );
--
-- CREATE DOMAIN public.did_suffix_type AS TEXT CHECK(VALUE ~ '^[0-9a-f]{64}$');
--
-- CREATE TABLE public.jwt_credentials(
--   "batch_id" VARCHAR(36) NOT NULL,
--   "credential_id" VARCHAR(36) NOT NULL,
--   "content" TEXT NOT NULL
-- );

CREATE TABLE public.issue_credential_records(
  "id" VARCHAR(36) NOT NULL PRIMARY KEY,
  "thid" VARCHAR(36) NOT NULL,
  "schema_id" VARCHAR(36),
  "subject_id" TEXT NOT NULL,
  "role"  VARCHAR(50) NOT NULL,
  "validity_period" INTEGER,
  "claims" TEXT NOT NULL,
  "protocol_state" VARCHAR(50) NOT NULL,
  "publication_state" VARCHAR(50),
  "offer_credential_data" TEXT,
  "request_credential_data" TEXT,
  "issue_credential_data" TEXT
);