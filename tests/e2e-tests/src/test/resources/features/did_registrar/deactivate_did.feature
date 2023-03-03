@DLT
Feature: Deactivate DID

Scenario: Deactivate DID
  Given Acme have published PRISM DID
  When Acme deactivates PRISM DID
  Then He sees that PRISM DID is successfully deactivated
