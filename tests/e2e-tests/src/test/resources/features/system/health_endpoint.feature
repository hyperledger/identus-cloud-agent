@system @smoke
Feature: Agent Health Endpoint

  Scenario: The version of a running Prism Agent can be retrieved from the Health Endpoint
    When Acme makes a request to the health endpoint
    Then Acme knows what version of the service is running
