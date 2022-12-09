Feature: Credential schemas

  Scenario: Successful schema creation
    When Acme creates a new credential schema
    Then New credential schema is available

  Scenario: Multiple schema creation
    When Acme creates 6 schemas
    Then All 6 schemas can be accessed with pagination 2

  Scenario: Schema creation failure with empty id
    When Acme creates a new schema with empty id
    Then New schema creation is failed with empty id error

#  # Not working for now (can be successfully created)
#  Scenario: Schema creation with 2 same IDs fails
#      When Acme creates a new schema with fixed id
#      And Acme tries to create a new schema with same id
#      Then Id duplicate error is thrown

#  # Not working for now (can be created with "" list)
#  Scenario: Schema creation failure with zero attributes
#      When Acme creates a new schema with zero attributes
#      Then New schema creation is failed with zero attributes error

#  # Not working for now (returns empty result with success)
#  Scenario: Get schemas with negative limit
#    When Acme tries to get schemas with negative limit
#    Then Negative limit error is thrown in response

#  # Not working for now (returns empty result with success)
#  Scenario: Get schemas with negative offset
#    When Acme tries to get schemas with negative offset
#    Then Wrong offset error is thrown in response
#
#  # Not working for now (returns empty result with success)
#  Scenario: Get schemas with offset greater than amount of schemas
#    When Acme tries to get schemas with offset greater than amount of schemas
#    Then Wrong offset error is thrown in response
