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

  Scenario Outline: Create issuer with <description> expect <httpStatus> response
    When Issuer tries to create oid4vci issuer with '<id>', '<url>', '<clientId>' and '<clientSecret>'
    Then Issuer should see the oid4vci '<httpStatus>' http status response with '<errorDetail>' detail
    Examples:
      | id                                   | url                | clientId | clientSecret | httpStatus | errorDetail                      | description        |
      | null                                 | null               | null     | null         | 400        | authorizationServer.url          | null values        |
      | null                                 | malformed          | id       | secret       | 400        | Relative URL 'malformed' is not  | malformed url      |
      | null                                 | http://example.com | id       | null         | 400        | authorizationServer.clientSecret | null client secret |
      | null                                 | http://example.com | null     | secret       | 400        | authorizationServer.clientId     | null client id     |
      | null                                 | null               | id       | secret       | 400        | authorizationServer.url          | null url           |
      | 4048ef76-749d-4296-8c6c-07c8a20733a0 | http://example.com | id       | secret       | 201        |                                  | right values       |
      | 4048ef76-749d-4296-8c6c-07c8a20733a0 | http://example.com | id       | secret       | 500        |                                  | duplicated id      |

  Scenario Outline: Update issuer with <description> expect <httpStatus> response
    Given Issuer has an existing oid4vci issuer
    When Issuer tries to update the oid4vci issuer '<property>' property using '<value>' value
    Then Issuer should see the oid4vci '<httpStatus>' http status response with '<errorDetail>' detail
    Examples:
      | property | value     | httpStatus | errorDetail | description |
      | url      | malformed | 404        |             | Invalid URL |
