ALTER TABLE public.wallet
ADD COLUMN "seed_digest" BYTEA;

-- Fill the seed hash with a dummy value just to make it uniuqe
UPDATE public.wallet
SET "seed_digest" = decode(replace("wallet_id"::text, '-', ''), 'hex')
WHERE "seed_digest" IS NULL;

ALTER TABLE public.wallet
ALTER COLUMN "seed_digest" SET NOT NULL;

ALTER TABLE public.wallet
ADD CONSTRAINT wallet_seed_digest UNIQUE ("seed_digest");
