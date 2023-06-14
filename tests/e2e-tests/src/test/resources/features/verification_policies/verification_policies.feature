@TEST_ATL-2487
Feature: Verification Policies

Scenario: Successful verification policy creation
  When Acme creates a new verification policy
  Then He sees new verification policy is available
  When He updates a new verification policy
  Then He sees the updated verification policy is available
  When He updates a new verification policy
  Then He sees the updated verification policy is available
