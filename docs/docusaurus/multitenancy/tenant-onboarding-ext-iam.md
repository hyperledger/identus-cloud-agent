# Tenant Onboarding with External IAM

In the [Tenant Onboarding](./tenant-onboarding.md) tutorial, we explored the basic
IAM functionality provided by the agent out of the box. Although it is usable and straightforward,
there are  more featureful tools available for handling identity and access management.
The agent has the capability to seamlessly connect with Keycloak as an external IAM system
allowing the application built on top to utilize capabilities that comes with Keycloak.

The PRISM Agent leverages standard protocols like OIDC and UMA for authentication and access management.
The user's identity is established through the ID token, and wallet permissions can be queried using the RPT (requesting party token).

## Roles

In tenant management with external IAM, there are 2 roles:

1. System administrator
2. Tenant

## Prerequisites

1. Keycloak up and running
2. Keycloak is configured as follows
   1. A realm called `my-realm` is created
   2. A client called `prism-agent` under `my-realm` with __authorization__ feature is created. (See [create client instruction](https://www.keycloak.org/docs/latest/authorization_services/index.html#_resource_server_create_client))
   3. Make sure the `prism-agent` client has __direct access grants__ enabled to simplify login process for this tutorial
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

This tutorial illustrates the process of provisioning a wallet resource for the new tenant and subsequently creating a tenant in Keycloak.
The administrator can then create a UMA permission for the wallet giving access to the tenant.

When setting up UMA permissions on the agent, the wallet resource along with the UMA policy and permission
are created on Keycloak according to a predefined convention.
For flexibility in defining custom policy and permission models,
administrators can manually create these UMA resources (resource, policy, permission) directly on Keycloak
using a set of UMA endpoints called [Protection API](https://www.keycloak.org/docs/latest/authorization_services/index.html#_service_protection_api).
However, using Protection API to manage permissions is out of scope for this tutorial.

Once the registration is successful, the tenant can obtain an ID token from Keycloak using any available OIDC flow,
such as the direct access grants (username & password). This ID token typically contains user claims such as username and subject ID.
The tenant can use Keycloak's token endpoint to convert this token to an RPT (requesting party token),
which is another token containing permissions information.
The tenant can access the multi-tenant agent by providing the RPT in the `Authorization` header.

## Endpoints

### Agent endpoints
| Endpoint                                   | Description                          | Role          |
|--------------------------------------------|--------------------------------------|---------------|
| `GET /wallets`                             | List the wallets on PRISM Agent      | Administrator |
| `POST /wallets`                            | Create a new wallet on PRISM Agent   | Administrator |
| `POST /wallets/{walletId}/uma-permissions` | Create a uma-permission for a wallet | Administrator |
| `GET /did-registrar/dids`                  | List the DIDs inside the wallet      | Tenant        |

### Keycloak endpoints
| Endpoint                                            | Description                   | Role                  |
|-----------------------------------------------------|-------------------------------|-----------------------|
| `POST /admin/realms/{realm}/users`                  | Create a new user on Keycloak | Administrator         |
| `GET /realms/{realm}/protocol/openid-connect/token` | Issue a new JWT token         | Administrator, Tenant |

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
The goal is to ensure the user is registered on Keycloak.
Keycloak offers great flexibility, allowing users to self-register,
connect to IDP, or be manually created by an administrator.
For this tutorial, the user will be manually created using Keycloak admin API for simplicity.

The first step is to get an admin token from Keycloak using the username and password.
This token allows the admin to perform operations on Keycloak such as creating a new user.
Running the provided command should return the admin access token.

```bash
curl -X 'POST' \
  'http://localhost:9980/realms/master/protocol/openid-connect/token' \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=$KEYCLOAK_ADMIN_USER" \
  -d "password=$KEYCLOAK_ADMIN_PASSWORD"
```

Make sure to replace the Keycloak variables with appropriate values.

Example token response (some fields omitted for readability)

```json
{
    "access_token": "eyJhbGciOi...7ocDHofUDQ",
    "refresh_token": "eyJhbGciOi...otsEEi4eQA",
    ...
}
```

After the admin get the `access_token` from Keycloak, a new user can be created by running this command.

```bash
curl -X 'POST' \
  'http://localhost:9980/admin/realms/my-realm/users' \
  -v \
  -H 'Authorization: Bearer eyJhbGciOi...7ocDHofUDQ' \
  -H 'Content-Type: application/json' \
  --data-raw "{
    \"username\": \"alice\",
    \"firstName\": \"Alice\",
    \"enabled\": true,
    \"credentials\": [{\"value\": \"1234\", \"temporary\": false}]
  }"
```

Make sure to use the correct `access_token` in the `Authorization` header from the previous command.

Example response log

```log
< HTTP/1.1 201 Created
< Referrer-Policy: no-referrer
< X-Frame-Options: SAMEORIGIN
< Strict-Transport-Security: max-age=31536000; includeSubDomains
< X-Content-Type-Options: nosniff
< X-XSS-Protection: 1; mode=block
< Location: http://localhost:9980/admin/realms/my-realm/users/205e04b7-0158-41b0-89c3-f91c3a09f89b
< content-length: 0
```

The response should return status `201 Created` indicating the new user is registerd with username `alice` with a password `1234`.
The user ID can be observed from `Location` header of the response. This ID will be used for creating permission later in this tutorial.

For in-depth user management, please consult the official Keycloaak administration documentation on [managing users section](https://www.keycloak.org/docs/latest/server_admin/index.html#assembly-managing-users_server_administration_guide).

### 4. Grant the user permission to the wallet

Once the user and wallet have been successfully created, the permissions can be created giving the user access to the wallet.
This can be done by invoking the `POST /wallets/{walletId}/uma-permissions` endpoint on the agent.

```bash
curl -X 'POST' \
  'http://localhost:8080/prism-agent/wallets/99734c87-5c9d-4697-b5fd-dea4e9590ba7/uma-permissions' \
  -H 'accept: */*' \
  -H 'x-admin-api-key: my-admin-token' \
  -H 'Content-Type: application/json' \
  -d '{
    "subject": "205e04b7-0158-41b0-89c3-f91c3a09f89b",
  }'
```

Make sure to use the correct `subject` for the user and the correct `walletId` from the step earlier.
The user ID can be observed from the response header from the previous step or in the __User__ menu in Keycloak Admin UI.

The response should return status `200 OK` in case of successful permission creation.

## Tenant interactions

After the user is registered on Keycloak and the required permission is created by admin,
the tenant can log in and utilize the agent by using the token issued by Keycloak.

### 1. Obtain access token from Keycloak

The first step is to authenticate via Keycloak through any applicable authentication method.
Usually, the tenant will use some frontend application that follows the standard flow for logging in.
For simplicity, a username and password flow will be used in this tutorial.
The administrator has already set up the username and password for the tenant.
The tenant can call Keycloak token endpoint directly with those credentials to get the access token.

Run the command to log in

```bash
curl -X 'POST' \
  'http://localhost:9980/realms/my-realm/protocol/openid-connect/token' \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=alice" \
  -d "password=1234"
```

Special attention on the `client_id`, this should be the actual `client_id` of the frontend application that log the user in.
For this tutorial, it is absolutely OK to use `admin-cli`.

Example token response (some fields omitted for readability)

```json
{
    "access_token": "eyJhbGciOi...7ocDHofUDQ",
    "refresh_token": "eyJhbGciOi...otsEEi4eQA",
    ...
}
```

### 2. Request RPT (requesting party token) from access token

After the access token is aquired, the next step is to get the RPT token which holds information about the permissions.
The RPT can be requested by running this command

```bash
curl -X POST \
  'http://localhost:9980/realms/my-realm/protocol/openid-connect/token' \
  -H "Authorization: Bearer eyJhbGciOi...7ocDHofUDQ" \
  -d "grant_type=urn:ietf:params:oauth:grant-type:uma-ticket" \
  -d "audience=prism-agent"
```

Example token response (some fields omitted for readability)

```json
{
    "access_token": "eyJhbGciOi...e7H6W8RUvA",
    "refresh_token": "eyJhbGciOi...W1_y1AF_YY",
    ...
}
```

Inspecting the token of the response, there should be a new claim in the JWT payload called `authorization`.

Example RPT payload (some fields omitted for readability)

```json
{
  ...
  "authorization": {
    "permissions": [
      {
        "rsid": "99734c87-5c9d-4697-b5fd-dea4e9590ba7",
        "rsname": "<WALLET_RESOURCE_NAME>"
      },
      ...
    ]
  },
  ...
}
```

### 3. Perform a simple action to verify access to PRISM Agent

To prove that the tenant can access the wallet,
try listing the DIDs in the wallet using RPT in the `Authorization` header.

```bash
curl --location --request GET 'http://localhost:8080/prism-agent/did-registrar/dids' \
  -H "Authorization: Bearer eyJhbGciOi...e7H6W8RUvA" \
  -H 'Accept: application/json'
```

The result should show 200 status with an empty list.
This means that the wallet has been created and it does not contain any DIDs.
Any interactions that the tenant performs should be scoped to only this wallet.

### A note on RPT

In this tutorial, there is an additional step for the tenant to request the RPT from the access token.
This aligns with the standard UMA interaction, where the handling of RPT typically occurs on the client side.
To simplify the experience, the agent has a feature that allows user to bypass this process.
By setting the variable `KEYCLOAK_UMA_AUTO_UPGRADE_RPT=true`, tenants can utilize the access token
obtained in step 1 directly in the `Authorization` header, eliminating the need for additional RPT request step.
