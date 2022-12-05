Feature: Issue Credentials

   @RFC0453 @AcceptanceTest
   Scenario: Issue a credential with the Issuer beginning with an offer with negotiation
     Given Acme and Bob agents
     And Acme and Bob have an existing connection
     And Acme offers a credential
     And Bob requests the credential
     And Acme issues the credential
     Then Bob has the credential issued
