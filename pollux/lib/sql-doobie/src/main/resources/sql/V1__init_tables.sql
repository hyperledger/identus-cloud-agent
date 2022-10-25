-- CREATE TYPE public.operation_type AS ENUM(
--   'CREATE',
--   'UPDATE',
--   'RECOVER',
--   'DEACTIVATE'
-- );
--
-- CREATE DOMAIN public.did_suffix_type AS TEXT CHECK(VALUE ~ '^[0-9a-f]{64}$');
--
CREATE TABLE public.jwt_credentials(
  "batch_id" VARCHAR(36) NOT NULL,
  "credential_id" VARCHAR(36) NOT NULL,
  "content" TEXT NOT NULL
);

CREATE TABLE public.issue_credential_records(
  "id" VARCHAR(36) NOT NULL PRIMARY KEY,
  "schema_id" VARCHAR(36) NOT NULL,
  "subject_id" TEXT NOT NULL,
  "validity_period" INTEGER NOT NULL,
  "claims" TEXT NOT NULL,
  "state" SMALLINT NOT NULL
);