ALTER TABLE public.did_secret_storage
  RENAME TO peer_did_rand_key;

-- Introduce DID breaking change given that we do not want to handle
-- migration complextiy and it's not used in production instance yet.
DELETE FROM public.peer_did_rand_key
  WHERE true;

DELETE FROM public.prism_did_hd_key
  WHERE true;

DELETE FROM public.prism_did_update_lineage
  WHERE true;

DELETE FROM public.prism_did_wallet_state
  WHERE true;

-- Introduce the concept of wallet
CREATE TABLE public.wallet(
  "wallet_id" SERIAL PRIMARY KEY,
  "created_at" TIMESTAMPTZ NOT NULL
);

CREATE TABLE public.wallet_seed(
  "wallet_id" INT PRIMARY KEY REFERENCES public.wallet("wallet_id"),
  "seed" BYTEA NOT NULL,
  "created_at" TIMESTAMPTZ NOT NULL
);

ALTER TABLE public.peer_did_rand_key
  ADD COLUMN "wallet_id" INT REFERENCES public.wallet("wallet_id") NOT NULL;

ALTER TABLE public.prism_did_wallet_state
  ADD COLUMN "wallet_id" INT REFERENCES public.wallet("wallet_id") NOT NULL;

-- Change contraints scope to only the same wallet
ALTER TABLE public.prism_did_wallet_state
  DROP CONSTRAINT prism_did_wallet_state_did_index_key;

ALTER TABLE public.prism_did_wallet_state
  ADD CONSTRAINT wallet_id_did_index UNIQUE(wallet_id, did_index);
