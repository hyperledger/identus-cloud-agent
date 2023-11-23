@DLT
Feature: Deactivate DID

Scenario: Deactivate DID
  Given Issuer have published PRISM DID
  When Issuer deactivates PRISM DID
  Then He sees that PRISM DID is successfully deactivated
