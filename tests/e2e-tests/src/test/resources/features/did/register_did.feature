Feature: Register DID

  Scenario: Successfully register a DID
    When I register a DID document
    Then the DID should be registered successfully

  Scenario: Read registered DIDs
    When I list registered DIDs
    Then the list should contain recently created DID
