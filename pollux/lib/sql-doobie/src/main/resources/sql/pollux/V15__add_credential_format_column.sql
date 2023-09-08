ALTER TABLE public.issue_credential_records
    ADD COLUMN "credential_format" VARCHAR(9);

UPDATE public.issue_credential_records
SET "credential_format" = 'JWT';

ALTER TABLE public.issue_credential_records
    ALTER COLUMN "credential_format" SET NOT NULL;

ALTER TABLE public.issue_credential_records
    ADD COLUMN "credential_definition_id" UUID;