CREATE TYPE public.did_publication_status AS ENUM(
  'CREATED',
  'PUBLICATION_PENDING',
  'PUBLISHED'
);

CREATE TABLE public.did_publication_state(
  "did" TEXT NOT NULL PRIMARY KEY,
  "publication_status" did_publication_status NOT NULL,
  "create_operation" BYTEA NOT NULL,
  "publish_operation_id" BYTEA
);
