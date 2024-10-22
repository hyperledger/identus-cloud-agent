@sdjwt @proof
Feature: Present SD-JWT Proof Protocol

  Scenario Outline: Holder presents sd-jwt proof to <verifier>
    Given <verifier> and Holder have an existing connection
    And Holder has a sd-jwt issued credential from Issuer
    When <verifier> sends a request for sd-jwt proof presentation to Holder requesting [firstName] claims
    And Holder receives the presentation proof request
    And Holder makes the sd-jwt presentation of the proof disclosing [firstName] claims
    Then <verifier> has the proof verified
    Examples:
      | verifier |
      | Verifier |
      | Issuer   |

  Scenario Outline: Holder presents a bound sd-jwt proof to <verifier>
    Given <verifier> and Holder have an existing connection
    And Holder has a bound sd-jwt issued credential from Issuer
    When <verifier> sends a request for sd-jwt proof presentation to Holder requesting [firstName] claims
    And Holder receives the presentation proof request
    And Holder makes the sd-jwt presentation of the proof disclosing [firstName] claims
    Then <verifier> has the sd-jwt proof verified
    Examples:
      | verifier |
      | Verifier |
      | Issuer   |

  Scenario Outline: Holder presents sd-jwt proof to <verifier>
    Given Holder has a sd-jwt issued credential from Issuer
    When <verifier> creates a OOB Invitation request for sd-jwt proof presentation requesting [firstName] claims
    And Holder accepts the OOB invitation request for JWT proof presentation from <verifier>
    And Holder receives the presentation proof request
    And Holder makes the sd-jwt presentation of the proof disclosing [firstName] claims
    Then <verifier> has the proof verified
    Examples:
      | verifier |
      | Verifier |
      | Issuer   |

#  Scenario: Holder presents sd-jwt proof with different claims from requested
#    Given Verifier and Holder have an existing connection
#    And Holder has a bound sd-jwt issued credential from Issuer
#    When Verifier sends a request for sd-jwt proof presentation to Holder requesting [firstName] claims
#    And Holder receives the presentation proof request
#    And Holder makes the sd-jwt presentation of the proof disclosing [lastName] claims
#    Then Verifier sees the proof returned verification failed

