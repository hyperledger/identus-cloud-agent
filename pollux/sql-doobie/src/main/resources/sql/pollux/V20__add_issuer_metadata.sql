CREATE TABLE public.issuer_metadata (
    id UUID PRIMARY KEY,
    authorization_server VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    wallet_id UUID NOT NULL
);

ALTER TABLE public.issuer_metadata
    ENABLE ROW LEVEL SECURITY;

CREATE POLICY issuer_metadata_wallet_isolation
    ON public.issuer_metadata
    USING (wallet_id = current_setting('app.current_wallet_id')::UUID);
