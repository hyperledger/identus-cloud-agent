@test
Feature: Credential schemas

@TEST_ATL-3835 
Scenario: Successful schema creation
  When Acme creates a new credential schema
  Then He sees new credential schema is available

@TEST_ATL-3836 
Scenario Outline: Multiple schema creation
  When Acme creates <schemas> new schemas
  Then He can access all of them one by one
Examples:
  | schemas |
  | 4       |
