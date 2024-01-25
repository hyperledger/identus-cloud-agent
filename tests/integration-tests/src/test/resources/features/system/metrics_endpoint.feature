@system @smoke
Feature: Metrics Endpoint

Scenario: Background job metrics are available to scrape
  When Issuer makes a request to the metrics endpoint
  Then Issuer sees that the metrics contain background job stats
