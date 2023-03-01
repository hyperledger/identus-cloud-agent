Feature: Manage DID

Scenario: Successfully create managed DID
  When Acme create a managed DID
  Then He sees the managed DID was created successfully

Scenario: DIDs should be stored
  Given Acme creates 5 managed DIDs
  When He lists all the managed DIDs
  Then He sees the list contains all created DIDs

Scenario Outline: Missing fields to create manage DID should fail
  Given Acme tries to create a managed DID with missing <field>
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

Scenario Outline: Wrong formatted fields to create manage DID should fail
  Given Acme tries to create a managed DID with value <value> in <field>
  Then He sees the request has failed with error status <error>
Examples:
  | field                                           | value  | error |
  | documentTemplate.publicKeys[0].id               | #      | 422   |
  | documentTemplate.publicKeys[0].purpose          | potato | 422   |
  | documentTemplate.services[0].id                 | #      | 422   |
  | documentTemplate.services[0].type               | potato | 422   |
  | documentTemplate.services[0].serviceEndpoint[0] | potato | 422   |
