Feature: Create and publish DID

Scenario: Create PRISM DID
  When Acme creates PRISM DID
  Then He sees PRISM DID was created successfully

Scenario: Successfully publish DID to ledger
  When Acme creates unpublished DID
  And He publishes DID to ledger
  Then He resolves DID document corresponds to W3C standard
