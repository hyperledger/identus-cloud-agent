CREATE TABLE public.VerifiableCredentialSchema
(
    id          UUID PRIMARY KEY default gen_random_uuid(),
    name        VARCHAR(128)             NOT NULL,
    version     VARCHAR(64)              NOT NULL,
    tags        VARCHAR(64) ARRAY        NULL,
    description TEXT                     NULL,
    attributes  VARCHAR(64) ARRAY        NOT NULL,
    author      VARCHAR(255)             NOT NULL,
    authored    TIMESTAMP WITH TIME ZONE NOT NULL,
    proof_id    UUID                     NULL,
    UNIQUE (name, version, author)
);

CREATE INDEX name_index ON VerifiableCredentialSchema (name);
CREATE INDEX version_index ON VerifiableCredentialSchema (version);
CREATE INDEX tags_index ON VerifiableCredentialSchema (tags);
CREATE INDEX attributes_index ON VerifiableCredentialSchema (attributes);
CREATE INDEX author_index ON VerifiableCredentialSchema (author);
CREATE INDEX authored_index ON VerifiableCredentialSchema (authored);

CREATE TABLE public.Proof
(
    id                 UUID primary key default gen_random_uuid(),
    type               VARCHAR(128)             NOT NULL,
    created            TIMESTAMP WITH TIME ZONE NOT NULL,
    verificationMethod VARCHAR(128)             NOT NULL,
    proofPurpose       VARCHAR(128)             NOT NULL,
    proofValue         TEXT                     NOT NULL,
    domain             VARCHAR(255)             NULL
);

