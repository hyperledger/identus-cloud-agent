@DLT @did @deactivate
Feature: Deactivate DID

Scenario: Deactivate DID
  Given Issuer creates empty unpublished DID
  And Issuer publishes DID to ledger
  When Issuer deactivates PRISM DID
  Then He sees that PRISM DID is successfully deactivated
