# Tenant Onboarding with External IAM

In the [Tenant Onboarding](./tenant-onboarding.md) tutorial, we explored the basic
IAM functionality provided by the agent out of the box. Although it is usable and straightforward,
there are robust and more powerful tools available for handling identity and access management.
The agent has the capability to seamlessly connect with Keycloak as an external IAM system.
The application built on top is able utilize any available OAuth flow configured on Keycloak.
The token issued by Keycloak can then be used for both authentication and authorization within the PRISM Agent.

The PRISM Agent leverages standard protocols like OIDC and UMA for authentication and resource access management.
The user's identity is established through the OIDC token, and resource permissions can be queried using the RPT (requesting party token).

## Roles

In tenant management with external IAM, there are 2 roles:

1. System administrator
2. Tenant

## Prerequisites

1. Keycloak up and running
2. Keycloak is configured as follows
   1. A realm called `my-realm` is created
   2. A client called `prism-agent` under `my-realm` with __authorization__ feature is created. (See [create client instruction](https://www.keycloak.org/docs/latest/authorization_services/index.html#_resource_server_create_client))
   3. Make sure the `prism-agent` client has __direct access grants__ enabled to simplify login process in this tutorial
3. PRISM Agent up and running
4. PRISM Agent is configured with the following environment variables:
   1. `ADMIN_TOKEN=my-admin-token`
   2. `DEFAULT_WALLET_ENABLED=false`
   3. `KEYCLOAK_ENABLED=true`
   4. `KEYCLOAK_URL=http://localhost:9980` (replace with appropriate value)
   5. `KEYCLOAK_REALM=my-realm`
   6. `KEYCLOAK_CLIENT_ID=prism-agent`
   7. `KEYCLOAK_CLIENT_SECRET=<KEYCLOAK_CLIENT_SECRET>` (replace with appropriate value)
   8. `KEYCLOAK_UMA_AUTO_UPGRADE_RPT=false`

## Overview

This is a guide on how to onboard a new tenant from scratch using Keycloak as an external IAM.
This tutorial will illustrate the process of creating a tenant in Keycloak and subsequently
provisioning a wallet resource for the new tenant by the administrator.
The administrator can then create a UMA permission for the wallet giving access to the tenant.

Once the registration is successful, the tenant can obtain an ID token from Keycloak using any available OAuth flow,
such as the direct access grants (username & password). This ID token typically contains user claims such as username or subject ID.
The tenant can utilize Keycloak's token endpoint to upgrade this token to an RPT (requesting party token),
which is another token containing permissions on permitted resources.
The tenant can access the multi-tenant agent by providing the RPT in the authorization header.

## Endpoints

### Agent endpoints
| Endpoint                                   | Description                          | Role          |
|--------------------------------------------|--------------------------------------|---------------|
| `GET /wallets`                             | List the wallets on PRISM Agent      | Administrator |
| `POST /wallets`                            | Create a new wallet on PRISM Agent   | Administrator |
| `POST /wallets/{walletId}/uma-permissions` | Create a uma-permission for a wallet | Administrator |
| `GET /did-registrar/dids`                  | List the DIDs inside the wallet      | Tenant        |

### Keycloak endpoints
| Endpoint                                            | Description                   | Role          |
|-----------------------------------------------------|-------------------------------|---------------|
| `POST /admin/realms/{realm}/users`                  | Create a new user on Keycloak | Administrator |
| `GET /realms/{realm}/protocol/openid-connect/token` | Issue a new JWT token         | All roles     |

## Administrator interactions

### 1. Check the existing wallets

When running PRISM Agent using the configurations above, the Agent should start with an empty state.
Listing wallets on it should return empty results.

```bash
curl -X 'GET' \
  'http://localhost:8080/prism-agent/wallets' \
  -H 'accept: application/json' \
  -H 'x-admin-api-key: my-admin-token'
```

Response Example:

```json
{
  "self": "/wallets",
  "kind": "WalletPage",
  "pageOf": "/wallets",
  "contents": []
}
```

### 2. Create a new wallet

The wallet can be created using a `POST /wallets` endpoint.
This wallet is going to act as a container for the tenant's assets (DIDs, VCs, Connections, etc.).
The wallet seed may be provided during the wallet creation or omitted to let the Agent generate one randomly.


```bash
curl -X 'POST' \
  'http://localhost:8080/prism-agent/wallets' \
  -H 'accept: application/json' \
  -H 'x-admin-api-key: my-admin-token' \
  -H 'Content-Type: application/json' \
  -d '{
    "seed": "c9994785ce6d548134020f610b76102ca1075d3bb672a75ec8c9a27a7b8607e3b9b384e43b77bb08f8d5159651ae38b98573f7ecc79f2d7e1f1cc371ce60cf8a",
    "name": "my-wallet"
  }'
```

Response Example:

```json
{
  "id": "99734c87-5c9d-4697-b5fd-dea4e9590ba7",
  "name": "my-wallet",
  "createdAt": "2023-01-01T00:00:00Z",
  "updatedAt": "2023-01-01T00:00:00Z"
}
```

### 3. User registration on Keycloak

There are multiple ways to complete this step.
The objective is to ensure the user is registered on Keycloak.
Keycloak offers flexibility in configuration, allowing users to self-register,
connect to identity providers, or be manually created by an administrator.
For this tutorial, the user will be manually created using Keycloak admin API for simplicity.

The first step involves getting an admin token from Keycloak using the admin username and password.
Running the provided command should return the admin access token.

```bash
curl -X 'POST' \
  'http://localhost:9980/realms/master/protocol/openid-connect/token' \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=$KEYCLOAK_ADMIN_USER" \
  -d "password=$KEYCLOAK_ADMIN_PASSWORD"
```

Replace the Keycloak variables with appropriate values.

Example token response (some fields omitted for readability)

```json
{
    "access_token": "eyJhbGciOi...7ocDHofUDQ",
    "refresh_token": "eyJhbGciOi...otsEEi4eQA",
    "token_type": "Bearer",
    ...
}
```

After the admin has obtained an `access_token` from Keycloak, a new user can be created by running this command.

```bash
curl -X 'POST' \
  'http://localhost:9980/admin/realms/my-realm/users' \
  -H 'Authorization: Bearer eyJhbGciOi...7ocDHofUDQ' \
  -H 'Content-Type: application/json' \
  --data-raw "{
    \"id\": \"alice\",
    \"username\": \"alice\",
    \"firstName\": \"Alice\",
    \"enabled\": true,
    \"credentials\": [{\"value\": \"1234\", \"temporary\": false}]
  }"
```

Make sure to use the correct `access_token` in the `Authorization` header from the previous command.

The response should return status `201 Created` indicating the new user is registerd with id `alice` with a password `1234`.

### 4. Grant the user permission to the wallet

## Tenant interactions

### 1. Obtain access token from Keycloak

### 2. Request RPT (requesting party token) from access token

### 3. Perform a simple action to verify access to PRISM Agent
