ALTER TABLE public.credential_status_lists
    ADD COLUMN status_list_credential JSON;

-- Set value to empty credential status list with valid proof
UPDATE public.credential_status_lists
SET status_list_credential = '{"proof":{"type":"DataIntegrityProof","proofPurpose":"assertionMethod","verificationMethod":"data:application/json;base64,eyJAY29udGV4dCI6WyJodHRwczovL3czaWQub3JnL3NlY3VyaXR5L211bHRpa2V5L3YxIl0sInR5cGUiOiJNdWx0aWtleSIsInB1YmxpY0tleU11bHRpYmFzZSI6InVNRmt3RXdZSEtvWkl6ajBDQVFZSUtvWkl6ajBEQVFjRFFnQUVRUENjM1M0X0xHVXRIM25DRjZ2dUw3ekdEMS13UmVrMHRHbnB0UnZUakhIMUdvTnk1UFBIZ0FmNTZlSzNOd3B0LWNGcmhrT2pRQk1rcFRKOHNaS1pCZz09In0=","created":"2024-01-22T22:40:34.560891Z","proofValue":"zAN1rKq8npnByRqPRxhjHEkivhN8AhA8V6MqDJga1zcCUEvPDUoqJB5Rj6ZJHTCnBZ98VXTEVd1rprX2wvP1MAaTEi7Pm241qm","cryptoSuite":"eddsa-jcs-2022"},"@context":["https://www.w3.org/2018/credentials/v1","https://w3id.org/vc/status-list/2021/v1"],"type":["VerifiableCredential","StatusList2021Credential"],"id":"https://example.com/credentials/status/3","issuer":"did:issuer:MDP8AsFhHzhwUvGNuYkX7T","issuanceDate":1705963233,"credentialSubject":{"id":"https://example.com/credentials/status/3#list","type":"StatusList2021","statusPurpose":"revocation","encodedList":"H4sIAAAAAAAA_-3BMQEAAADCoPVPbQwfoAAAAAAAAAAAAAAAAAAAAIC3AYbSVKsAQAAA"}}';

-- Step 3: Add the NOT NULL constraint
ALTER TABLE public.credential_status_lists
    ALTER COLUMN status_list_credential SET NOT NULL;

-- Step 4: Drop the old column
ALTER TABLE public.credential_status_lists
    DROP COLUMN status_list_jwt_credential;
