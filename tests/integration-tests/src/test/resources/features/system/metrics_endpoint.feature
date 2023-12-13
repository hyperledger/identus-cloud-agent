@system @smoke
Feature: Agent Health Endpoint

Scenario: Background job metrics are produced by the service and available to scrape
  When Issuer makes a request to the metrics endpoint
  Then Issuer sees that the metrics contain background job stats
