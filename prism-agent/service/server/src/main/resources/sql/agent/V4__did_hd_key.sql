CREATE TYPE public.prism_did_key_usage AS ENUM(
  'MASTER',
  'ISSUING',
  'KEY_AGREEMENT',
  'AUTHENTICATION',
  'REVOCATION',
  'CAPABILITY_INVOCATION',
  'CAPABILITY_DELEGATION'
);

CREATE TYPE public.prism_did_key_mode AS ENUM(
  'RANDOM',
  'HD'
);

-- add key derivation path to secret storage table
ALTER TABLE public.prism_did_secret_storage
ALTER COLUMN "key_pair" DROP NOT NULL,
ADD COLUMN "key_mode" PRISM_DID_KEY_MODE,
ADD COLUMN "hd_key_usage" PRISM_DID_KEY_USAGE,
ADD COLUMN "hd_key_index" INT;

UPDATE public.prism_did_secret_storage
SET "key_mode" = 'RANDOM';

ALTER TABLE public.prism_did_secret_storage
ALTER COLUMN "key_mode" SET NOT NULL;
