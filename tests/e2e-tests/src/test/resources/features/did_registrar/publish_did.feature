@DLT
Feature: Publish DID

@TEST_ATL-3842
Scenario: Successfully publish DID to ledger
  When Acme creates unpublished DID
  And He publishes DID to ledger
  Then He resolves DID document corresponds to W3C standard
