Feature: Vc Verification schemas

Scenario: Successful Verifies VcVerificationRequest
  Given Issuer and Holder have an existing connection
  When Issuer verifies VcVerificationRequest
