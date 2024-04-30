Feature: Vc Verification schemas

  Background:
    When Issuer creates unpublished DID

  Scenario: Successful Verifies VcVerificationRequest
    When Issuer verifies VcVerificationRequest
