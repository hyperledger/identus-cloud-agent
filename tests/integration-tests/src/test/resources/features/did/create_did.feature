@DLT @did @create
Feature: Create and publish DID

Scenario Outline: Create PRISM DID
  When Issuer creates PRISM DID with <curve> key having <purpose> purpose
  Then He sees PRISM DID was created successfully
Examples:
  | curve   | purpose         |
  | secp256k1 | authentication  |
  | secp256k1 | assertionMethod |
  | Ed25519   | authentication  |
  | Ed25519   | assertionMethod |
  | X25519    | keyAgreement    |

Scenario Outline: Create PRISM DID with disallowed key purpose
  When Issuer creates PRISM DID with <curve> key having <purpose> purpose
  Then He sees PRISM DID was not successfully created
 Examples:
   | curve   | purpose         |
   | Ed25519 | keyAgreement    |
   | X25519  | authentication  |
   | X25519  | assertionMethod |

Scenario: Successfully publish DID to ledger
  When Issuer creates unpublished DID
  And He publishes DID to ledger
  Then He resolves DID document corresponds to W3C standard
