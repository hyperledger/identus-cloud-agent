import test, { after, describe } from "node:test";

describe.skip("Error Handling Report Problem for Agent - General", async () => {

    describe("G1 - Receive a message for an unsupported protocol", async () => {
        test("When Didcomm receives a message with an unsupported protocol", async () => {
        })
        
        test("Then the expected problem report should be 'e.p.msg.unsupported'", async () => {
        })
    })

    describe("G2 - Receive a message for an unsupported version of the protocol", async () => {
        test("When Issuer receives a message with an unsupported version of the protocol", async () => {
        })

        test("Then the expected problem report should be 'e.p.msg.unsupported'", async () => {
        })

        test("And the expected problem description should contain 'what version(s) its supported'", async () => {
        })
    })

    describe("G3 - When an internal error or any unexpected error happens", async () => {
        // When Issuer receives an unexpected message
        // Then the expected problem report should be 'e.p.error'
    })

    describe("G4 - If the message is tampered or got any crypto errors when decoding", async () => {
        // Scenario: 
        // When Issuer receives a message that is tampered
        // Then the expected problem report should be 'e.p.trust.crypto'
    })

    describe("G5 - If the DID method is not supported (did.peer in this case)", async () => {
        // Scenario:
        // When Issuer receives a did method that is not supported
        // Then the expected problem report should be 'e.p.did'

    })

    describe("G6 - If the DID method is malformed", async () => {
        // Scenario: 
        // When Issuer receives a did that is malformed
        // Then the expected problem report should be 'e.p.did.malformed'

    })

    describe("G7 - When a parsing error from the decrypted message", async () => {
        // Scenario: 
        // When Issuer receives a malformed message
        // Then the expected problem report should be 'e.p.msg.<PIURI>'
        // And the expected problem description should contain 'message is malformed or if parsing into a specific protocol's data model fails.'
    })
})
