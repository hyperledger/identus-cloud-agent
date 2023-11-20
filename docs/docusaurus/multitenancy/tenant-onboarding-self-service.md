# Tenant Onboarding Self-Service

In the [Tenant Onboarding with External IAM](./tenant-onboarding-ext-iam.md) tutorial,
we learned how Keycloak helps with user access and how it works together with the agent.
To set things up, the admin has to provision the required resources.
However, relying on the admin for onboarding operation can be restrictive for some use cases.
For example, some tenants might want to onboard on a self-service agent instance without admin intervention.

By leveraging Keycloak for a self-service agent instance,
users can self-register or link to other Identity Providers (IDPs) to register an account.
Once the account is registered, users can use it to set up their own wallet.
This tutorial will dive into the steps to facilitate this scenario where administrator intervention is not needed.

## Roles

In sef-service tenant management with external IAM, there is only 1 role:

1. Tenant

## Prerequisites

1. Keycloak up and running
2. Keycloak is configured as follows
   1. A realm called `my-realm` is created
   2. A client called `prism-agent` under `my-realm` with __authorization__ feature is created. (See [create client instruction](https://www.keycloak.org/docs/latest/authorization_services/index.html#_resource_server_create_client))
   3. Make sure the `prism-agent` client has __direct access grants__ enabled to simplify login process for this tutorial
   4. Make sure to [allow user self-registration](https://www.keycloak.org/docs/latest/server_admin/index.html#con-user-registration_server_administration_guide).
3. PRISM Agent up and running
4. PRISM Agent is configured with the following environment variables:
   1. `ADMIN_TOKEN=my-admin-token`
   2. `DEFAULT_WALLET_ENABLED=false`
   3. `KEYCLOAK_ENABLED=true`
   4. `KEYCLOAK_URL=http://localhost:9980` (replace with appropriate value)
   5. `KEYCLOAK_REALM=my-realm`
   6. `KEYCLOAK_CLIENT_ID=prism-agent`
   7. `KEYCLOAK_CLIENT_SECRET=<KEYCLOAK_CLIENT_SECRET>` (replace with appropriate value)
   8. `KEYCLOAK_UMA_AUTO_UPGRADE_RPT=true`

## Overview

This tutorial demonstrate the process of user self-registration on Keycloak.
Then the users can log in to Keycloak to obtain a token.
When this token is used on the agent for the wallet creation, the agent recognizes it belonging to a tenant and
automatically associate the tenant's permission with the created wallet.

## Endpoints

### Agent endpoints
| Endpoint                                   | Description                          | Role          |
|--------------------------------------------|--------------------------------------|---------------|
| `GET /wallets`                             | List the wallets on PRISM Agent      | Tenant        |
| `POST /wallets`                            | Create a new wallet on PRISM Agent   | Tenant        |
| `POST /wallets/{walletId}/uma-permissions` | Create a uma-permission for a wallet | Tenant        |
| `GET /did-registrar/dids`                  | List the DIDs inside the wallet      | Tenant        |

### Keycloak endpoints
| Endpoint                                            | Description                   | Role                  |
|-----------------------------------------------------|-------------------------------|-----------------------|
| `GET /realms/{realm}/protocol/openid-connect/token` | Issue a new JWT token         | Administrator, Tenant |

## Tenant interactions

### 1. Self-register account on Keycloak

Start by registering a new account on Keycloak, accessible through its login page.
Usually this should be available at `http://localhost:9980/realms/my-realm/account/`.

For detailed instruction on how to register a new user,
please refer to [registering a new user](https://www.keycloak.org/docs/latest/server_admin/index.html#proc-registering-new-user_server_administration_guide) section on the official documentation.

### 2. Obtain access token from Keycloak

Once a new account is registered, Keycloak should recognize it, allowing the user to log in and obtain the access token.

Run this command to log in and get the access token

```bash
curl -X 'POST' \
  'http://localhost:9980/realms/my-realm/protocol/openid-connect/token' \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=alice" \
  -d "password=1234"
```

Make sure to use the correct username and password of the user.
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

### 3. Check the existing wallets

### 4. Create a new wallet

### 5. Perform a simple action to verify access to PRISM Agent
