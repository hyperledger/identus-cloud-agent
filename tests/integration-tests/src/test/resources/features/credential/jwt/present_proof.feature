@jwt @proof
Feature: Present Proof Protocol

  Scenario: Holder presents jwt credential proof to verifier
    Given Verifier and Holder have an existing connection
    And Holder has a jwt issued credential from Issuer
    When Verifier sends a request for jwt proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder makes the jwt presentation of the proof
    Then Verifier has the proof verified

  Scenario: Holder presents jwt proof to verifier which is the issuer itself
    Given Issuer and Holder have an existing connection
    And Holder has a jwt issued credential from Issuer
    When Issuer sends a request for jwt proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder makes the jwt presentation of the proof
    Then Issuer has the proof verified

  Scenario: Verifier rejects holder proof
    Given Verifier and Holder have an existing connection
    And Holder has a jwt issued credential from Issuer
    When Verifier sends a request for jwt proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder rejects the proof
    Then Holder sees the proof is rejected

  Scenario Outline: Verifying jwt credential using <assertionMethod> assertion and <authenticationName> authentication method
    Given Issuer and Holder have an existing connection
    And Verifier and Holder have an existing connection
    And Holder creates unpublished DID for JWT
    When Issuer prepares a custom PRISM DID
    And Issuer adds a '<assertionMethod>' key for 'assertionMethod' purpose with '<assertionName>' name to the custom PRISM DID
    And Issuer adds a '<authentication>' key for 'authentication' purpose with '<authenticationName>' name to the custom PRISM DID
    And Issuer creates the custom PRISM DID
    When Issuer offers a jwt credential to Holder with "long" form DID using issuingKid "<assertionName>"
    And Holder receives the credential offer
    And Holder accepts jwt credential offer using 'auth-1' key id
    And Issuer issues the credential
    Then Holder receives the issued credential
    When Verifier sends a request for jwt proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder makes the jwt presentation of the proof
    Then Verifier has the proof verified
    Examples:
      | assertionMethod | assertionName | authentication | authenticationName |
      | secp256k1       | assert-1      | secp256k1      | auth-1             |
      | secp256k1       | assert-1      | ed25519        | auth-1             |
      | ed25519         | assert-1      | ed25519        | auth-1             |
      | ed25519         | assert-1      | ed25519        | auth-1             |

  Scenario: Connectionless Verification Holder presents jwt credential proof to verifier
    Given Holder has a jwt issued credential from Issuer
    When Verifier creates a OOB Invitation request for JWT proof presentation
    And Holder accepts the OOB invitation request for JWT proof presentation from Verifier
    And Holder receives the presentation proof request
    And Holder makes the jwt presentation of the proof
    Then Verifier has the proof verified
