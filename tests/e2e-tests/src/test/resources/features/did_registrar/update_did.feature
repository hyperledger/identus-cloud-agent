Feature: Update DID

Background:
  Given Acme have published PRISM DID for updates

Scenario: Update PRISM DID by adding new services
  When Acme updates PRISM DID with new services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated with new services

Scenario: Update PRISM DID by removing services
  When Acme updates PRISM DID by removing services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated by removing services

Scenario: Update PRISM DID by updating services
  When Acme updates PRISM DID by updating services
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated by updating services

Scenario: Update PRISM DID by adding new keys
  When Acme updates PRISM DID by adding new keys
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated with new keys

Scenario: Update PRISM DID by removing keys
  When Acme updates PRISM DID by removing keys
  And He submits PRISM DID update operation
  Then He sees PRISM DID was successfully updated and keys removed
