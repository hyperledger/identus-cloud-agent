@DLT
Feature: Deactivate DID

Scenario: Deactivate DID
  Given Acme have published PRISM DID for deactivation
  When Acme deactivates PRISM DID
  Then He sees that PRISM DID is successfully deactivated
