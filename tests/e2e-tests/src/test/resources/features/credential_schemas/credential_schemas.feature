@schemas
Feature: Credential schemas

  Scenario: Successful schema creation
    When Acme creates a new credential schema
    Then He sees new credential schema is available

  Scenario Outline: Multiple schema creation
    When Acme creates <schemas> new schemas
    Then He can access all of them one by one
    Examples:
      | schemas |
      | 4       |

#Scenario Outline: Wrong specified fields for schema generation requests should fail
#  When Acme tries to create a new schema with <value> in field <field>
#  Then He sees the request with status <status>
#Examples:
#  | field      | value | status |
#  | id         | -1    | 400    |
#  | attributes | null  | 400    |
#
#@skip
#Scenario: Schema creation with identical name should fail
#
#@skip @bug
#Scenario: Schema creation with identical id should fail
#  When Acme creates a new schema with some id
#  And Acme tries to create a new schema with identical id
#  Then He sees the request failure with identical id error

#Scenario Outline: Wrong specified fields for schema generation requests should fail
#  When Acme tries to create a new schema with <value> in field <field>
#  Then He sees the request with status <status>
#Examples:
#  | field   | value | status |
#  | name    | null  | 400    |
#  | version | null  | 400    |
#  | schema  | null  | 400    |
#
#@skip @bug
#Scenario Outline: Using incorrect filter params should result in error
#  When Acme tries to get schemas with <value> in parameter <parameter>
#  Then He sees the request with status <status>
#Examples:
#  | parameter | value   | status |
#  | limit     | -1      | 400    |
#  | offset    | -1      | 400    |
#  | offset    | 1000000 | 400    |
