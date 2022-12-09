Feature: Manage DID

  Scenario: Successfully create managed DID
    When I create a managed DID
    Then it should be created successfully

  Scenario: Read managed DIDs
    When I list managed DIDs
    Then it should contain the recently created DID

  Scenario: Publish managed DID
    When I publish the recently created DID
    Then it should be published
