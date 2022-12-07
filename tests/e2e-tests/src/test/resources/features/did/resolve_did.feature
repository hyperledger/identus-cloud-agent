Feature: Resolve DID

  Scenario: Successful DID resolve
    When I resolve existing DID by DID reference
    Then Response code is 200
    And I achieve standard compatible DID document
