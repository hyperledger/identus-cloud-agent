@anoncred @proof
Feature: Present Proof Protocol

Scenario: Holder presents anoncreds credential proof to verifier
  Given Issuer and Holder have an existing connection
  And Verifier and Holder have an existing connection
  And Issuer has a published DID for 'ANONCRED'
  And Holder has an unpublished DID for 'ANONCRED'
  And Issuer has an anoncred schema definition
  And Issuer offers anoncred to Holder
  And Holder receives the credential offer
  And Holder accepts anoncred credential offer
  And Issuer issues the credential
  And Holder receives the issued credential
  When Verifier sends a anoncreds request for proof presentation to Holder using credential definition issued by Issuer
  And Holder receives the presentation proof request
  And Holder accepts the anoncreds presentation request
#  Then Verifier has the proof verified FIXME
