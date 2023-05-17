-- add key management mode to prism did
CREATE TYPE public.prism_did_key_mode AS ENUM(
  'RANDOM',
  'HD'
);

ALTER TABLE public.prism_did_wallet_state
ADD COLUMN "key_mode" PRISM_DID_KEY_MODE,
ADD COLUMN "did_index" INT UNIQUE;

UPDATE public.prism_did_wallet_state
SET "key_mode" = 'RANDOM'
WHERE "key_mode" IS NULL;

ALTER TABLE public.prism_did_wallet_state
ALTER COLUMN "key_mode" SET NOT NULL;

-- add hd key related tables
CREATE TYPE public.prism_did_key_usage AS ENUM(
  'MASTER',
  'ISSUING',
  'KEY_AGREEMENT',
  'AUTHENTICATION',
  'REVOCATION',
  'CAPABILITY_INVOCATION',
  'CAPABILITY_DELEGATION'
);

CREATE TABLE public.prism_did_hd_key(
  "did" TEXT NOT NULL,
  "key_id" TEXT NOT NULL,
  "key_usage" PRISM_DID_KEY_USAGE,
  "key_index" INT,
  CONSTRAINT fk_did FOREIGN KEY ("did") REFERENCES public.prism_did_wallet_state("did") ON DELETE RESTRICT
);
