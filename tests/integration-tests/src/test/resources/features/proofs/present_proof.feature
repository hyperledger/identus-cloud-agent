@proof @jwt
Feature: Present Proof Protocol

  Scenario: Holder presents credential proof to verifier
    Given Verifier and Holder have an existing connection
    And Holder has a jwt issued credential from Issuer
    When Verifier sends a request for proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder makes the presentation of the proof
    Then Verifier has the proof verified

  Scenario: Holder presents proof to verifier which is the issuer itself
    Given Issuer and Holder have an existing connection
    And Holder has a jwt issued credential from Issuer
    When Issuer sends a request for proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder makes the presentation of the proof
    Then Issuer has the proof verified

  Scenario: Verifier rejects holder proof
    Given Verifier and Holder have an existing connection
    And Holder has a jwt issued credential from Issuer
    When Verifier sends a request for proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder rejects the proof
    Then Holder sees the proof is rejected

