--  peer did  and wallet id mapping
CREATE TABLE public.did_wallet_id_mapping (
    "did" TEXT NOT NULL PRIMARY KEY,
    "wallet_id" UUID NOT NULL,
    "created_at"  TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at"  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_wallet_id ON public.did_wallet_id_mapping(wallet_id);