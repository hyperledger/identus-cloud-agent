Feature: Resolve DID

  Scenario: Successful DID resolve
    Given 2 agents
      | name | role   |
      | Acme | issuer |
      | Bob  | holder |
    When I resolve existing DID by DID reference
    Then Response code is 200
    And I achieve standard compatible DID document
