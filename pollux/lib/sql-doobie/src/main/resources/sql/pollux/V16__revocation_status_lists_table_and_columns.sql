CREATE TABLE public.credential_revocation_status_lists
(
    id                     UUID PRIMARY KEY                  default gen_random_uuid(),
    wallet_id              UUID                     NOT NULL,
    status_list_credential JSON                     NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL default now(),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL default now()
);
CREATE INDEX credential_revocation_status_lists_wallet_id_index ON public.credential_revocation_status_lists (wallet_id);


ALTER TABLE public.issue_credential_records
    ADD COLUMN revocation_status_list_id UUID    default NULL,
    ADD CONSTRAINT revocation_status_list_id_fkey FOREIGN KEY (revocation_status_list_id) REFERENCES public.credential_revocation_status_lists (id) ON DELETE CASCADE ON UPDATE CASCADE,
    ADD COLUMN status_list_index         INTEGER default NULL,
    ADD COLUMN revoked                   BOOLEAN default NULL;

