@RFC0453 @AIP20
Feature: Issue Credentials Protocol

Scenario: Issuing credential with published PRISM DID
  Given Acme and Bob have an existing connection
  When Acme creates unpublished DID
  And He publishes DID to ledger
  And Bob creates unpublished DID
  And Acme offers a credential to Bob with "short" form DID
  And Bob receives the credential offer
  And Bob accepts credential offer for JWT
  And Acme issues the credential
  Then Bob receives the issued credential

Scenario: Issuing credential with unpublished PRISM DID
  Given Acme and Bob have an existing connection
  When Acme creates unpublished DID
  And Bob creates unpublished DID
  And Acme offers a credential to Bob with "long" form DID
  And Bob receives the credential offer
  And Bob accepts credential offer for JWT
  And Acme issues the credential
  Then Bob receives the issued credential

Scenario: Issuing anoncred with published PRISM DID
  Given Acme and Bob have an existing connection
  When Acme creates unpublished DID
  And He publishes DID to ledger
  And Bob creates unpublished DID
  And Acme creates anoncred schema
  And Acme creates anoncred credential definition
  And Acme offers anoncred to Bob
  And Bob receives the credential offer
  And Bob accepts credential offer for anoncred
  And Acme issues the credential
  Then Bob receives the issued credential
