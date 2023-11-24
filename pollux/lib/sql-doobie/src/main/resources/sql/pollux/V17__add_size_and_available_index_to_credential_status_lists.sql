-- Adding columns 'size' and 'last_used_index' to table 'credential_status_lists'
ALTER TABLE public.credential_status_lists
    -- https://www.w3.org/TR/vc-status-list/#revocation-bitstring-length (minimum suggested bitstring length)
    ADD COLUMN size            INTEGER NOT NULL DEFAULT 131072,
    ADD COLUMN last_used_index INTEGER NOT NULL DEFAULT 0;

-- Rename the column 'encoded_list' to 'encoded_list_credential'
ALTER TABLE public.credential_status_lists
    RENAME COLUMN encoded_list TO status_list_credential;

-- Change the data type of the 'encoded_list_credential' column to JSON
ALTER TABLE public.credential_status_lists
    ALTER COLUMN status_list_credential TYPE JSON USING status_list_credential::JSON;
