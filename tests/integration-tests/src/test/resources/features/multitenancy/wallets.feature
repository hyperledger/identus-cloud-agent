@multitenancy
Feature: Wallets management

Scenario Outline: Successful creation of a new wallet
  When Admin creates new wallet with name '<name>'
  Then Admin should have a wallet with name '<name>'
Examples:
  | name       |
  | "wallet-1" |
  | "2"        |

Scenario: Create a wallet with an existing name is allowed
  Given Admin creates new wallet with unique name
  When Admin creates new wallet with the same unique name
  Then Admin should have two wallets with unique name but different ids

Scenario: Create a wallet with the same id is not allowed
  Given Admin creates new wallet with unique id
  When Admin creates new wallet with the same unique id
  Then Admin should have only one wallet and second operation should fail

Scenario: Create a wallet with wrong seed is not allowed
  Given Admin creates new wallet with wrong seed
  Then Admin should see the error and wallet should not be created
