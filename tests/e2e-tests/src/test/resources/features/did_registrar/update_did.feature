@DLT
Feature: Update DID

Background: Published DID is created
  #@PRECOND_ATL-3843
  Given Acme have published PRISM DID

@TEST_ATL-3844
Scenario: Update PRISM DID by adding new services
  When Acme updates PRISM DID with new services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated with new services

@TEST_ATL-3845
Scenario: Update PRISM DID by removing services
  When Acme updates PRISM DID by removing services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated by removing services

@TEST_ATL-3846
Scenario: Update PRISM DID by updating services
  When Acme updates PRISM DID by updating services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated by updating services

@TEST_ATL-3847
Scenario: Update PRISM DID by adding new keys
  When Acme updates PRISM DID by adding new keys
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated with new keys

@TEST_ATL-3848
Scenario: Update PRISM DID by removing keys
  When Acme updates PRISM DID by removing keys
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated and keys removed
