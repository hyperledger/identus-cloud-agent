-- Remove unused tables?
DROP TABLE public.proof;
DROP TABLE public.verifiable_credential_schema;

-- Introduce breaking change given that we do not want to handle
-- migration complextiy and it's not used in production instance yet.
DELETE FROM public.credential_schema
WHERE true;

DELETE FROM public.verification_policy_constraint
WHERE true;

DELETE FROM public.verification_policy
WHERE true;

DELETE FROM public.presentation_records
WHERE true;

DELETE FROM public.issue_credential_records
WHERE true;

-- Introduce a concept of wallet
ALTER TABLE public.credential_schema
ADD COLUMN "wallet_id" UUID NOT NULL;

ALTER TABLE public.verification_policy
ADD COLUMN "wallet_id" UUID NOT NULL;

ALTER TABLE public.issue_credential_records
ADD COLUMN "wallet_id" UUID NOT NULL;

ALTER TABLE public.presentation_records
ADD COLUMN "wallet_id" UUID NOT NULL;
