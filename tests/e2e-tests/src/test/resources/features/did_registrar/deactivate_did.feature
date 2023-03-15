@DLT
Feature: Deactivate DID

@TEST_ATL-3837 
Scenario: Deactivate DID
  Given Acme have published PRISM DID
  When Acme deactivates PRISM DID
  Then He sees that PRISM DID is successfully deactivated
