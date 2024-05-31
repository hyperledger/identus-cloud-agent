@did @list
Feature: DID listing

Scenario: Listing multiple PRISM DIDs
  Given Issuer creates 5 PRISM DIDs
  When He lists all PRISM DIDs
  Then He sees the list contains all created DIDs
