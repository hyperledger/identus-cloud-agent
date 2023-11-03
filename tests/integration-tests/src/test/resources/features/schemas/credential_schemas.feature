Feature: Credential schemas

Scenario: Successful schema creation
  When Acme creates unpublished DID
  And Acme creates a new credential schema
  Then He sees new credential schema is available

Scenario Outline: Multiple schema creation
  When Acme creates unpublished DID
  And Acme creates <schemas> new schemas
  Then He can access all of them one by one
Examples:
  | schemas |
  | 4       |
