-- Include the credentials_to_use to create a verifiable presentation 
ALTER TAbLE public.presentation_records
    ADD COLUMN "credentials_to_use" TEXT[] NULL;