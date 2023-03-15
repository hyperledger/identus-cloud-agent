Feature: Create DID

@TEST_ATL-3838 
Scenario: Create PRISM DID
  When Acme creates PRISM DID
  Then He sees PRISM DID was created successfully

@TEST_ATL-3839 
Scenario Outline: PRISM DID creation fails when required request fields are missing
  Given Acme tries to create PRISM DID with missing <field>
  Then He sees the request has failed with error status <error>
Examples:
  | field                                        | error |
  | documentTemplate                             | 400   |
  | documentTemplate.publicKeys                  | 400   |
  | documentTemplate.publicKeys[0].id            | 400   |
  | documentTemplate.publicKeys[0].purpose       | 400   |
  | documentTemplate.services                    | 400   |
  | documentTemplate.services[0].id              | 400   |
  | documentTemplate.services[0].type            | 400   |
  | documentTemplate.services[0].serviceEndpoint | 400   |

@TEST_ATL-3840 
Scenario Outline: PRISM DID creation fails with wrong formatted fields
  Given Acme tries to create a managed DID with value <value> in <field>
  Then He sees the request has failed with error status <error>
Examples:
  | field                                           | value  | error |
  | documentTemplate.publicKeys[0].id               | #      | 422   |
  | documentTemplate.publicKeys[0].purpose          | potato | 422   |
  | documentTemplate.services[0].id                 | #      | 422   |
  | documentTemplate.services[0].type               | potato | 422   |
  | documentTemplate.services[0].serviceEndpoint[0] | potato | 422   |
