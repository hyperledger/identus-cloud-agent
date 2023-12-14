Feature: Present Proof Protocol

Scenario: Holder presents credential proof to verifier
  Given Verifier and Holder have an existing connection
  And Holder has an issued credential from Issuer
  When Verifier sends a request for proof presentation to Holder
  And Holder receives the request
  And Holder makes the presentation of the proof to Verifier
  Then Verifier has the proof verified

Scenario: Verifier rejects holder proof
  Given Verifier and Holder have an existing connection
  And Holder has an issued credential from Issuer
  When Verifier sends a request for proof presentation to Holder
  And Holder receives the request
  And Holder rejects the proof
  Then Holder sees the proof is rejected

Scenario: Holder presents proof to verifier which is the issuer itself
  Given Issuer and Holder have an existing connection
  And Holder has an issued credential from Issuer
  When Issuer sends a request for proof presentation to Holder
  And Holder receives the request
  And Holder makes the presentation of the proof to Issuer
  Then Issuer has the proof verified
