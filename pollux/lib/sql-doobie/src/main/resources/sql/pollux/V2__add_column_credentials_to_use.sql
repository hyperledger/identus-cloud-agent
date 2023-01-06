-- Include the credentials_to_use to create a verifiable presentation 
ALTER TABLE public.presentation_records
    ADD COLUMN "credentials_to_use" TEXT[] NULL;