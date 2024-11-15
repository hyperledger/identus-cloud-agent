@DLT @did @update
Feature: Update DID

  Background: Published DID is created
    Given Issuer has a published DID for 'JWT'

  Scenario: Update PRISM DID services
    When Issuer updates PRISM DID with new services
    Then He sees the PRISM DID should have been updated successfully
    And He sees that PRISM DID should have the new service

    When Issuer updates PRISM DID by updating services
    Then He sees the PRISM DID should have been updated successfully
    And He sees the PRISM DID should have the service updated

    When Issuer updates PRISM DID by removing services
    Then He sees the PRISM DID should have been updated successfully
    And He sees the PRISM DID should have the service removed

  Scenario Outline: Update PRISM DID keys using <curve> for <purpose>
    When Issuer updates PRISM DID by adding new key with <curve> curve and <purpose> purpose
    Then He sees PRISM DID was successfully updated with new keys of <purpose> purpose

    When Issuer updates PRISM DID by removing keys
    Then He sees PRISM DID was successfully updated and keys removed with <purpose> purpose
    Examples:
      | curve     | purpose         |
      | secp256k1 | authentication  |
      | secp256k1 | assertionMethod |
      | Ed25519   | authentication  |
      | Ed25519   | assertionMethod |
      | X25519    | keyAgreement    |

  Scenario Outline: Update PRISM DID with disallowed key purpose
    When Issuer updates PRISM DID by adding new key with <curve> curve and <purpose> purpose
    Then He sees the PRISM DID was not successfully updated
    Examples:
      | curve   | purpose         |
      | Ed25519 | keyAgreement    |
      | X25519  | authentication  |
      | X25519  | assertionMethod |
