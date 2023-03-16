@system @smoke
Feature: Agent Health Endpoint

@TEST_ATL-3833
Scenario: The runtime version can be retrieved from the Health Endpoint
  When Acme makes a request to the health endpoint
  Then Acme knows what version of the service is running
