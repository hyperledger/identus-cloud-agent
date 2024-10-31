import { describe } from "node:test";

describe.skip("Error Handling Report Problem for Agent - Verification", async () => {
    describe("V1 - Credential Format Mismatch: The verifier expects a credential in a specific format, but the prover presents it in a different format.", async () => {
        // V1 Send a problem report e.p.msg.credential-format-mismatch

    })
    describe("V2 - Credentials presented have signatures that cannot be verified. This might be due to the credential being tampered with, or the public key used for verification being incorrect.", async () => {
        //    V2 Send a problem report e.p.msg.invalid-signature
    })

    describe("V3 - Revoked Credentials: The prover presents a credential that has been revoked by the issuer", async () => {
        //    V3 Send a problem report e.p.msg.revoked-credentials
    })
    
    describe("V4 - Expired Credentials: The credentials presented are past their expiry date, making them invalid for the transaction.", async () => {
        //    V4 Send a problem report e.p.msg.expired-credentials
    })

    describe("V5 - Mismatch in the Proof Request and Presentation: The verifier's proof request asks for certain attributes or predicates, but the presentation from the prover doesn't match these requirements.", async () => {
        //    V5 Send a problem report e.p.msg.proof-mismatch
    })

    describe("V6 - Schema Mismatch: The verifier expects a credential that adheres to a certain schema, but the credential presented follows a different schema.", async () => {
        //    V6 Send a problem report e.p.msg.schema-mismatch
    })

    describe("V7 - Credential Format Mismatch: The verifier expects a credential in a specific format, but the prover presents it in a different format.", async () => {
        //    V7 Send a problem report e.p.msg.revoked-or-expired-issuer-key
    })

    describe("V8 - Max retries (Cascading Problems): Verification State cannot be moved after max retries", async () => {
        //    V8 Send a problem report (After max retries) e.p.req.max-retries-exceeded
    })

    describe("V9 - See G3", async () => {
        //    V9(G3) Send a problem report Any other error e.p.error
    })

    describe("V10 - Database connection or related issue", async () => {
        //    V10 Send a problem report (DB connection issues) e.p.me.res.storage
    })
})

