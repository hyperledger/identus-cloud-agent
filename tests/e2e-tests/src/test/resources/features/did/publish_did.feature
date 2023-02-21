Feature: Publish DID

Scenario: Successfully publish DID to ledger
  Given Acme creates unpublished DID
  When He publishes DID to ledger
  Then He sees DID successfully published to ledger
  And He resolves DID document corresponds to W3C standard
