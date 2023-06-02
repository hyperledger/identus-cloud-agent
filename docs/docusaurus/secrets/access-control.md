# Access Control

**Status**: draft

**Date**: 2021-06-02

**NOTE**: this document is a draft and is not implemented yet. Statement in this document might be changed in the future.

## Introduction

Current document describes the levels of access control in the PRISM platform configured in the Vault secret service
The Vault service uses policies to control the access to the secrets, configuration, and other resources.
The policies are applied to the entities and groups of entities.

According to the Principle of Least Privilege (PoLP), the access to the resources must be granted to the entities with the minimal set of permissions required to perform the operation.

## Accounts

### SUDO Account

The account with `sudo` privileges that allows to configure the Vault service.
The root token is used for this purpose.
Can be used for development and testing purposes only.
Must be kept in the most safe place and not used for regular requests as it has access to all resources managed by the Vault.

SUDO Account is used for the following purposes:
- configure the Vault service
- configure authentication methods
- configure Management Accounts

Managed by DevOps and SRE teams.

### Management Account

Management Account is created for and used by Atala tribe members.
GitHub authentication and teleport access must be configured for this account.

The account with the limited access to configure the Vault service with the following permissions:
- create the Wallet and Tenants accounts
- create the Agent account
- enforces the policies to the tenant account

### Agent Account

Agent Account is created for and used by the PRISM Agent.
AppRole authentication method is used for this account.

The account with the limited access to configure the Vault service with the following permissions:
- create the Wallet account
- issue the token to the Wallet account

### Wallet Account

The Wallet Account is created for and used by the Wallet, and is associated with the Tenant.
The Wallet Account is has access to the secrets of the Tenant only.
This account has the following permissions:
- list the secrets associated with the Tenant
- read the secrets associated with the Tenant
- write the secrets associated with the Tenant
- delete the secrets associated with the Tenant

### Tenant Account

Tenant Account is created for and used by the Tenant to authenticate itself to the PRISM platform from Web and Mobile application.
Tenant account must be linked to the Wallet account to have access to the secrets.

## Technical Overview

### Principle of Least Privilege

The Principle of Least Privilege (PoLP) is a security concept that requires that a user is granted no more privilege than necessary to perform a task.
The following practices are applied to implement the PoLP:
- audit: all the credentials are audited and must be under control of the SRE team
- administrator and business accounts are separated
- activity monitoring: activity of the administrator accounts is monitored
- just-in-time access: the access to the resources is granted only for the time required to perform the operation

**NOTE**: there are other PoLP practices that are not covered in this document. These practices will be ignored for local development and testing purposes.

In order to implement the PoLP, the following access control rules are defined:
- PRISM Agent account has access to the Wallet account that belong to the Agent only
- PRISM Agent account transparently issues the token to the Wallet account based on the Tenant account

### Token Issuing, Renewal, Expiration and Revocation

These policies are applied to all tokens except the SUDO account (root token).

All tokens issued by the PRISM platform have the following properties:
- expiration time
- maximum time to live (TTL)
- renewable flag
- orphan flag
- policies
- operation limitations (number of the requests that can be performed by the token)

Management account token policies:
- authentication methods: GitHub, GoogleAuth, user/password
- expiration time: 1 hour

Agent Account token policies:
- authentication method: AppRole
- token expiration time: 24 hour

Wallet Account token policies:
- authentication methods: JWT, token issued by the Agent account
- token expiration time: 1 hour

Tenant account token policies:
- authentication methods: JWT/OIDC, GitHub, GoogleAuth, user/password
- token expiration time: 1 hour

**NOTE**: user/password method is used for the development and testing purposes only.

## Links

- [Vault Policies](https://www.vaultproject.io/docs/concepts/policies)
- [Vault Tokens](https://www.vaultproject.io/docs/concepts/tokens)
- [Vault Authentication Methods](https://www.vaultproject.io/docs/auth)
- [Vault AppRole Authentication Method](https://www.vaultproject.io/docs/auth/approle)
- [Vault JWT Authentication Method](https://www.vaultproject.io/docs/auth/jwt)
- [Vault GitHub Authentication Method](https://www.vaultproject.io/docs/auth/github)
- [Vault GoogleAuth Authentication Method](https://www.vaultproject.io/docs/auth/google)
- [Vault Userpass Authentication Method](https://www.vaultproject.io/docs/auth/userpass)
- [Vault Tokens](https://www.vaultproject.io/docs/concepts/tokens)