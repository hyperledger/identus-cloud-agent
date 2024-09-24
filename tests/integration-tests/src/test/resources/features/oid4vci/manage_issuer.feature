@oid4vci
Feature: Manage OID4VCI credential issuer

  Scenario: Successfully create credential issuer
    When Issuer creates an oid4vci issuer
    Then Issuer sees the oid4vci issuer exists on the agent
    And Issuer sees the oid4vci issuer on IssuerMetadata endpoint

  Scenario: Successfully update credential issuer
    Given Issuer has an existing oid4vci issuer
    When Issuer updates the oid4vci issuer
    Then Issuer sees the oid4vci issuer updated with new values
    And Issuer sees the oid4vci IssuerMetadata endpoint updated with new values

  Scenario: Successfully delete credential issuer
    Given Issuer has an existing oid4vci issuer
    When Issuer deletes the oid4vci issuer
    Then Issuer cannot see the oid4vci issuer on the agent
    And Issuer cannot see the oid4vci IssuerMetadata endpoint

  @test
  Scenario Outline: Create issuer with wrong data should not work
    When Issuer tries to create oid4vci issuer with '<id>', '<url>', '<clientId>' and '<clientSecret>'
    Then Issuer should see the oid4vci error '<error>'
    Examples:
      | id    | url   | clientId | clientSecret | error |
      | null  | null  | null     | null         |       |
      |       |       |          |              |       |
      | empty | empty | empty    | empty        |       |
      | 1     | {}    | null     | null         |       |
