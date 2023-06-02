# Account Management

**Status**: draft

**Date**: 2021-06-02

**NOTE**: this document is a draft and is not implemented yet. Statement in this document might be changed in the future.


## Introduction

This document describes the account management in the PRISM platform, types of accounts, and their usage, account authentication, and logical isolation of accounts.

## Technical Overview

Account management is a set of operations that allow users to manage their accounts.
Account is required for both single and multi-tenant configurations.
In the PRISM platform, the account owns the corresponding Wallet managed by the Agent.
The account is identified by the tenant ID and represented by the Entity in the Vault service.

### Account Types

The PRISM platform supports the following types of accounts:
- PRISM Agent Account - application account used by the PRISM Agent to authenticate itself to the Vault service
- Wallet Account - application account used by the Wallet to authenticate itself to the Vault service
- Tenant Account - user account used by the user to authenticate mobile or WEB application to the PRISM Platform.

**NOTE**: 

Both types of accounts must be created in the Vault to guarantee logical isolation between tenants and groups of agents.

### Account Creation

The PRISM Agent Account is created by before the start of the PRISM Agent using the Vault cli, REST API or WEB UI.
The Wallet account can be created on the go by the Agent or using the Vault cli, REST API or WEB UI.
The User account is linked to the Wallet account but uses a different authentication mechanism.

### PRISM Agent Account

The Agent account required to authenticate the instance of the Agent.
The account is responsible for creating the Wallet accounts and issuing the tokens to the Wallet instance.
The Agent account doesn't have the access to the Wallet secrets.
PRISM Agent uses [AppRole](https://www.vaultproject.io/docs/auth/approle) authentication method to authenticate itself to the Vault service.

### Wallet Account

The Wallet account is required to authenticate the instance of the Wallet and logically isolate the Wallet secrets from other Wallets.
The Wallet uses token authentication method to access the Vault REST API.
The Wallet is initialized by the Agent on demand and internally communicates with the Agent to reissue the access token.
The Wallet account must guarantee the data isolation between tenants in the multi-tenant configuration by leveraging the policies of the Vault service and Row Based Policies in the PostgreSQL database.

### Account Authentication

Account authentication is a process of authenticating the account to the PRISM platform from the WEB or Mobile application.
The account is linked to the Wallet, so the same logical data isolation is applied to the account.
JWT token is used to authenticate the account to the PRISM platform.

**NOTE**: It's still discussed which component will be responsible for issuing the JWT token. The Vault service looks promising for configuring OIDC authentication method and use 3rd party system for authentication.

### Account Deactivation

The Wallet account together with User account can be deactivated by using the Vault REST API or cli.

## Links
- [Vault AppRole](https://www.vaultproject.io/docs/auth/approle)
- [Vault Entities and Groups](https://developer.hashicorp.com/vault/tutorials/auth-methods/identity)
- [Vault JWT/OIDC](https://developer.hashicorp.com/vault/api-docs/auth/jwt)