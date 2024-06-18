@verification @api
Feature: Vc Verification schemas

#  Scenario: Receive a jwt vc and verify it
#    Given Holder has an issued credential from Issuer
#    And Holder uses that JWT VC issued from Issuer for Verification API
#    And Holder sends the JWT Credential to Issuer Verification API
#      | ALGORITHM_VERIFICATION    | true   |
#      | EXPIRATION_CHECK          | true   |
#      | ISSUER_IDENTIFICATION     | true   |
#      | NOT_BEFORE_CHECK          | true   |
#      | SCHEMA_CHECK              | true   |
#      | SIGNATURE_VERIFICATION    | true   |
#      | SEMANTIC_CHECK_OF_CLAIMS  | true   |
#    Then Holder should see that all checks have passed

  Scenario: Expect all checks to pass
    Given Holder has a JWT VC for Verification API
    When Holder sends the JWT Credential to Issuer Verification API
      | ALGORITHM_VERIFICATION   | true  |
      | AUDIENCE_CHECK           | true  |
      | EXPIRATION_CHECK         | true  |
      | ISSUER_IDENTIFICATION    | true  |
      | NOT_BEFORE_CHECK         | true  |
      | SCHEMA_CHECK             | false |
      | SIGNATURE_VERIFICATION   | false |
      | SEMANTIC_CHECK_OF_CLAIMS | true  |
    Then Holder should see that all checks have passed

#  Scenario Outline: Expected failures
#    Given Holder has a <problem> problem in the Verifiable Credential
#    When Holder sends the <problem> JWT Credential to Issuer Verification API
#      | ALGORITHM_VERIFICATION    | true |
#      | AUDIENCE_CHECK            | true |
#      | COMPLIANCE_WITH_STANDARDS | true |
#      | EXPIRATION_CHECK          | true |
#      | INTEGRITY_OF_CLAIMS       | true |
#      | ISSUER_IDENTIFICATION     | true |
#      | NOT_BEFORE_CHECK          | true |
#      | REVOCATION_CHECK          | true |
#      | SCHEMA_CHECK              | true |
#      | SIGNATURE_VERIFICATION    | true |
#      | SUBJECT_VERIFICATION      | true |
#      | SEMANTIC_CHECK_OF_CLAIMS  | true |
#    Then Holder should see that verification has failed with <problem> problem
#    Examples:
#      | problem                   |
#      | AUDIENCE_CHECK            |
#      | COMPLIANCE_WITH_STANDARDS |
#      | EXPIRATION_CHECK          |
#      | INTEGRITY_OF_CLAIMS       |
#      | ISSUER_IDENTIFICATION     |
#      | NOT_BEFORE_CHECK          |
#      | REVOCATION_CHECK          |
#      | SCHEMA_CHECK              |
#      | SIGNATURE_VERIFICATION    |
#      | SUBJECT_VERIFICATION      |
#      | SEMANTIC_CHECK_OF_CLAIMS  |

#  Scenario: Unsupported verification check should fail
#    Given Holder has a JWT VC for Verification API
#    When Holder sends the JWT Credential to Issuer Verification API
#      | ALGORITHM_VERIFICATION    | true |
#      | AUDIENCE_CHECK            | true |
#      | COMPLIANCE_WITH_STANDARDS | true |
#      | EXPIRATION_CHECK          | true |
#      | INTEGRITY_OF_CLAIMS       | true |
#      | ISSUER_IDENTIFICATION     | true |
#      | NOT_BEFORE_CHECK          | true |
#      | REVOCATION_CHECK          | true |
#      | SCHEMA_CHECK              | true |
#      | SIGNATURE_VERIFICATION    | true |
#      | SUBJECT_VERIFICATION      | true |
#      | SEMANTIC_CHECK_OF_CLAIMS  | true |
#    Then Holder should see that all checks have passed
#    Examples:
#      | problem                   |
#      | AUDIENCE_CHECK            |
#      | COMPLIANCE_WITH_STANDARDS |
#      | EXPIRATION_CHECK          |
#      | INTEGRITY_OF_CLAIMS       |
#      | ISSUER_IDENTIFICATION     |
#      | NOT_BEFORE_CHECK          |
#      | REVOCATION_CHECK          |
#      | SCHEMA_CHECK              |
#      | SIGNATURE_VERIFICATION    |
#      | SUBJECT_VERIFICATION      |
#      | SEMANTIC_CHECK_OF_CLAIMS  |
