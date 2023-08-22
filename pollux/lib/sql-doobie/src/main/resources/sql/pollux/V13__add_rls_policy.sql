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

-- Enforce RLS
ALTER TABLE public.credential_schema ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.verification_policy ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.issue_credential_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.presentation_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.verification_policy_constraint ENABLE ROW LEVEL SECURITY;

CREATE POLICY credential_schema_wallet_isolation
ON public.credential_schema
USING (wallet_id = current_setting('app.current_wallet_id')::UUID);

CREATE POLICY verification_policy_wallet_isolation
ON public.verification_policy
USING (wallet_id = current_setting('app.current_wallet_id')::UUID);

CREATE POLICY issue_credential_records_wallet_isolation
ON public.issue_credential_records
USING (wallet_id = current_setting('app.current_wallet_id')::UUID);

CREATE POLICY presentation_records_wallet_isolation
ON public.presentation_records
USING (wallet_id = current_setting('app.current_wallet_id')::UUID);

CREATE POLICY verification_policy_constraint_wallet_isolation
ON public.verification_policy_constraint
USING (
    EXISTS (
        SELECT 1
        FROM public.verification_policy AS p
        WHERE
            p.wallet_id = current_setting('app.current_wallet_id')::UUID
            AND p.id = public.verification_policy_constraint.fk_id
    )
);
