Feature: Present Proof Protocol

@TEST_ATL-3850
Scenario: Holder presents credential proof to verifier
  Given Faber and Bob have an existing connection
  And Bob has an issued credential from Acme
  When Faber sends a request for proof presentation to Bob
  And Bob receives the request
  And Bob makes the presentation of the proof to Faber
  And Faber acknowledges the proof
  Then Faber has the proof verified

@TEST_ATL-3881
Scenario: Verifier rejects holder proof
  Given Faber and Bob have an existing connection
  And Bob has an issued credential from Acme
  When Faber sends a request for proof presentation to Bob
  And Bob receives the request
  And Bob rejects the proof
  Then Bob sees the proof is rejected

@TEST_ATL-4968
Scenario: Holder presents proof to verifier which is the issuer itself
  Given Acme and Bob have an existing connection
  And Bob has an issued credential from Acme
  When Acme sends a request for proof presentation to Bob
  And Bob receives the request
  And Bob makes the presentation of the proof to Acme
  And Acme acknowledges the proof
  Then Acme has the proof verified
