@jwt @revocation
Feature: JWT Credential revocation

  Background:
    Given Holder has a jwt issued credential from Issuer

  Scenario: Revoke jwt issued credential
    When Issuer revokes the credential issued to Holder
    Then Issuer should see the credential was revoked
    When Issuer sends a request for jwt proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder makes the jwt presentation of the proof
    Then Issuer sees the proof returned verification failed

  Scenario: Holder tries to revoke jwt credential from issuer
    When Holder tries to revoke credential from Issuer
    And Issuer sends a request for jwt proof presentation to Holder
    And Holder receives the presentation proof request
    And Holder makes the jwt presentation of the proof
    Then Issuer has the proof verified
    And Issuer should see the credential is not revoked
