-- Adding columns 'size' and 'last_used_index' to table 'credential_status_lists'
ALTER TABLE public.credential_status_lists
    -- https://www.w3.org/TR/vc-status-list/#revocation-bitstring-length (minimum suggested bitstring length)
    ADD COLUMN size            INTEGER NOT NULL DEFAULT 131072,
    ADD COLUMN last_used_index INTEGER NOT NULL DEFAULT 0;

-- Rename the column 'encoded_list' to 'status_list_jwt_credential'
ALTER TABLE public.credential_status_lists
    RENAME COLUMN encoded_list TO status_list_jwt_credential;

-- Remove the column "proof"
ALTER TABLE public.credential_status_lists
    DROP COLUMN proof
