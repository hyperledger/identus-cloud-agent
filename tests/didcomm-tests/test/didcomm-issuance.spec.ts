import { describe } from "node:test";

describe.skip("Error Handling Report Problem for Agent - Issuance", async () => {
    describe("I1 - Credential format mismatch: The Holder expects a credential format schema, but the credential issued is different format", async () => {
        // I1 Send a problem report e.p.msg.credential-format-mismatch
    })

    describe("I2 - Credential signature that cannot be verified. This might be due to the credential being tampered with, or the public key used for signing being incorrect or expired.", async () => {
        // I2 Send a problem report e.p.msg.invalid-signature
    })

    describe("I3 - Schema Mismatch: The Holder expects a credential that adheres to a certain schema, but the credential presented follows a different schema.", async () => {
        // I3 Send a problem report e.p.msg.schema-mismatch

    })

    describe("I4 - Database connection or related issue", async () => {
        // I4 Send a problem report (DB connection issues) e.p.me.res.storage

    })

    describe("I5 - Max retries (Cascading Problems): Issuance state cannot be moved after max retries", async () => {
        // I5 Send a problem report (After max retries) e.p.req.max-retries-exceeded

    })

    describe("I6 - See G3", async () => {
        // I6(G3) Send a problem report Any other error e.p.error
    })
})