@DLT
Feature: Publish DID

@TEST_ATL-3842
Scenario: Successfully publish DID to ledger
  Given Acme creates unpublished DID
  When He publishes DID to ledger
  And He resolves DID document corresponds to W3C standard
