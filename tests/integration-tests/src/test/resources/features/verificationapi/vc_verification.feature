@verification @api
Feature: Vc Verification schemas

  Scenario: Receive a jwt vc from cloud-agent and verify it
    Given Holder has a jwt issued credential with STUDENT_SCHEMA schema from Issuer
    And Holder uses that JWT VC issued from Issuer for Verification API
    And Holder sends the JWT Credential to Issuer Verification API
      | ALGORITHM_VERIFICATION   | true |
      | EXPIRATION_CHECK         | true |
      | ISSUER_IDENTIFICATION    | true |
      | NOT_BEFORE_CHECK         | true |
      | SCHEMA_CHECK             | true |
      | SIGNATURE_VERIFICATION   | true |
      | SEMANTIC_CHECK_OF_CLAIMS | true |
    Then Holder should see that all checks have passed

  Scenario: Expected checks for generated JWT VC
    Given Holder has a JWT VC for Verification API
    When Holder sends the JWT Credential to Issuer Verification API
      | ALGORITHM_VERIFICATION   | true  |
      | AUDIENCE_CHECK           | true  |
      | EXPIRATION_CHECK         | true  |
      | ISSUER_IDENTIFICATION    | true  |
      | NOT_BEFORE_CHECK         | true  |
      | SIGNATURE_VERIFICATION   | false |
      | SEMANTIC_CHECK_OF_CLAIMS | true  |
    Then Holder should see that all checks have passed

  Scenario Outline: Expected failures
    Given Holder has a <problem> problem in the Verifiable Credential
    When Holder sends the JWT Credential to Issuer Verification API
      | <problem> | false |
    Then Holder should see that verification has failed with <problem> problem
    Examples:
      | problem                  |
      | ALGORITHM_VERIFICATION   |
      | AUDIENCE_CHECK           |
      | EXPIRATION_CHECK         |
      | ISSUER_IDENTIFICATION    |
      | NOT_BEFORE_CHECK         |
      | SIGNATURE_VERIFICATION   |
      | SEMANTIC_CHECK_OF_CLAIMS |

  Scenario Outline: Unsupported verification check should fail
    Given Holder has a JWT VC for Verification API
    When Holder sends the JWT Credential to Issuer Verification API
      | <verification> | false |
    Then Holder should see the check has failed
    Examples:
      | verification              |
      | COMPLIANCE_WITH_STANDARDS |
      | INTEGRITY_OF_CLAIMS       |
      | REVOCATION_CHECK          |
      | SUBJECT_VERIFICATION      |
