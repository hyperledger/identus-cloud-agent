@verification @api
Feature: Verification API

  Scenario: Verify a jwt credential using verification api
    Given Holder has a jwt issued credential with 'STUDENT_SCHEMA' schema from Issuer
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

  Scenario: Verify a pre-generated jwt credential using verification api
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

  Scenario Outline: Verify credential with <problem> problem
    Given Holder has a '<problem>' problem in the Verifiable Credential
    When Holder sends the JWT Credential to Issuer Verification API
      | <problem> | false |
    Then Holder should see that verification has failed with '<problem>' problem
    Examples:
      | problem                  |
      | ALGORITHM_VERIFICATION   |
      | AUDIENCE_CHECK           |
      | EXPIRATION_CHECK         |
      | ISSUER_IDENTIFICATION    |
      | NOT_BEFORE_CHECK         |
      | SIGNATURE_VERIFICATION   |
      | SEMANTIC_CHECK_OF_CLAIMS |

  Scenario Outline: Unsupported verification <verification> check should fail
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
