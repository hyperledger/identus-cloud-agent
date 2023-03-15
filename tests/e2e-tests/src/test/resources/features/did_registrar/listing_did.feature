@ATL-3851
Feature: DID listing

@TEST_ATL-3841 
Scenario: Listing multiple PRISM DIDs
  Given Acme creates 5 PRISM DIDs
  When He lists all PRISM DIDs
  Then He sees the list contains all created DIDs
