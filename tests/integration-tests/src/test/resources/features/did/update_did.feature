@DLT
Feature: Update DID

Background: Published DID is created
  Given Issuer have published PRISM DID

Scenario: Update PRISM DID by adding new services
  When Issuer updates PRISM DID with new services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated with new services

Scenario: Update PRISM DID by removing services
  When Issuer updates PRISM DID by removing services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated by removing services

Scenario: Update PRISM DID by updating services
  When Issuer updates PRISM DID by updating services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated by updating services

Scenario: Update PRISM DID by adding new keys
  When Issuer updates PRISM DID by adding new keys
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated with new keys

Scenario: Update PRISM DID by removing keys
  When Issuer updates PRISM DID by removing keys
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated and keys removed
