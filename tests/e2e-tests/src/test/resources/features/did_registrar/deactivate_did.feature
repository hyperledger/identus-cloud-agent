Feature: Deactivate DID

Background:
  Given Acme have published PRISM DID for deactivation

Scenario: Deactivate DID
  When Acme deactivates PRISM DID
  Then He sees that PRISM DID is successfully deactivated
