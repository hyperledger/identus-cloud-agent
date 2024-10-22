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

  Scenario: Connectionless Verification Holder presents jwt credential proof to verifier
    Given Holder has a jwt issued credential from Issuer
    When Verifier creates a OOB Invitation request for JWT proof presentation
    And Holder accepts the OOB invitation request for JWT proof presentation from Verifier
    And Holder receives the presentation proof request
    And Holder makes the jwt presentation of the proof
    Then Verifier has the proof verified