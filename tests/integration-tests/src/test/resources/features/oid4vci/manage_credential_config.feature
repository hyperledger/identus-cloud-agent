@oid4vci
Feature: Manage OID4VCI credential configuration

  Background:
    Given Issuer has a published DID for JWT
    And Issuer has published STUDENT_SCHEMA schema
    And Issuer has an existing oid4vci issuer

  Scenario: Successfully create credential configuration
    When Issuer uses STUDENT_SCHEMA to create a credential configuration "StudentProfile"
    Then Issuer sees the "StudentProfile" configuration on IssuerMetadata endpoint

  Scenario: Successfully delete credential configuration
    Given Issuer has "StudentProfile" credential configuration created from STUDENT_SCHEMA
    When Issuer deletes "StudentProfile" credential configuration
    Then Issuer cannot see the "StudentProfile" configuration on IssuerMetadata endpoint

  Scenario Outline: Create configuration with <description> expect <httpStatus> code
    When Issuer creates a new credential configuration request
    And Issuer uses <issuerId> issuer id for credential configuration
    And Issuer adds '<configurationId>' configuration id for credential configuration request
    And Issuer adds '<format>' format for credential configuration request
    And Issuer adds '<schemaId>' schemaId for credential configuration request
    And Issuer sends the create a credential configuration request
    Then Issuer should see that create credential configuration has failed with '<httpStatus>' status code and '<errorDetail>' detail
    Examples:
      | issuerId | configurationId | format       | schemaId         | httpStatus | errorDetail     | description           |
      | wrong    | StudentProfile  | jwt_vc_json  | STUDENT_SCHEMA   | 500        |                 | wrong issuer id       |
      | existing | null            | jwt_vc_json  | STUDENT_SCHEMA   | 400        | configurationId | null configuration id |
      | existing | StudentProfile  | null         | STUDENT_SCHEMA   | 400        | format          | null format           |
      | existing | StudentProfile  | wrong-format | STUDENT_SCHEMA   | 400        | format          | wrong format          |
      | existing | StudentProfile  | jwt_vc_json  | null             | 400        | schemaId        | null schema           |
      | existing | StudentProfile  | jwt_vc_json  | malformed-schema | 502        |                 | malformed schema      |
      | existing | StudentProfile  | jwt_vc_json  | STUDENT_SCHEMA   | 201        |                 | wrong issuer id       |
      | existing | StudentProfile  | jwt_vc_json  | STUDENT_SCHEMA   | 500        |                 | wrong issuer id       |

  Scenario: Delete non existent credential configuration
    When Issuer deletes a non existent "NonExistentProfile" credential configuration
    Then Issuer should see that create credential configuration has failed with '404' status code and 'There is no credential configuration' detail
