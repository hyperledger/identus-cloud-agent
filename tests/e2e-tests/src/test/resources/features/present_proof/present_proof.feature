#@RFC0037
#Feature: Present Proof Protocol
#
#  @AIP20
#  Scenario: Holder presents credential proof to verifier
#    Given Faber and Bob have an existing connection
#    And Bob has an issued credential from Acme
#    When Faber sends a request for proof presentation to Bob
#    And Bob makes the presentation of the proof to Faber
#    And Faber acknowledges the proof
#    Then Faber has the proof verified
#
#  @skip
#  Scenario: Accepting already accepted proof should fail
#    Given Faber and Bob have an existing connection
#    And Bob has an issued credential from Acme
#    When Faber sends a request for proof presentation to Bob
#    And Bob makes the presentation of the proof to Faber
#    And Bob sends already sent proof
#    Then Bob checks that the request failed
