# Use JWT claims for agent admin access control

- Status: accepted
- Deciders: Pat Losoponkul, Yurii Shynbuiev, David Poltorak, Shailesh Patil
- Date: 2024-01-03
- Tags: multitenancy, authorisation, authentication

Technical Story: [Allow agent admin role to be authenticated by Keycloak JWT | https://input-output.atlassian.net/browse/ATL-6074]

## Context and Problem Statement

Administrators currently rely on a static API key configured in the agent.
Employing a static API key for administrative operations poses a security challenge for the entire system.
A standardized centralized permission management system for administrative operations should be integrated,
to ensure the solution is security-compliant, yet remains extensible and decoupled.

The existing tenant authorization model relies on UMA (user-managed access) to protect the wallet.
In addition to the wallet usage, the agent also handles wallet management,
a functionality utilized by both tenants and administrators.
While administrators don't directly use the wallet, they oversee its management.
Integrating an auth model to distinguish between admins and tenants presents a new challenge.

- Where and how to define the role of admin and tenant?
- What should be the authorization model for the admin role?
- What boundary the admin role should be scoped to?
- How to support different deployment topologies?
- How does it interact with the wallet UMA model?

## Decision Drivers

- Must not prevent us from using other IAM systems
- Must not prevent us from supporting fine-grained tenant wallet access in the future
- Should not mix admin access with tenant access
- Should be easy to setup, configure and maintain

## Considered Options

1. Use `ClientRole` for defining roles in Keycloak

    In this option, the `ClientRole` is configured at the client level,
    and the user is mapped to the `ClientRole` using a role mapper.
    The role claim will be available in the JWT token at `resource_access.<client_id>.roles`.

2. Use `RealmRole` for defining roles in Keycloak

    In this option, the `RealmRole` is configured at the realm level,
    and the user is mapped to the `RealmRole` using a role mapper.
    The role claim will be available in the JWT token at `realm_acces.roles`.

3. Use custom user attribute for defining roles in Keycloak

    In this option, the role is defined as a user attribute.
    Then the user attribute will be included in a token using a token mapper at any pre-configured path.

## Decision Outcome

Option 1: Use `ClientRole` for defining roles in keycloak.

Example JWT payload containing `ClientRole`. (Some claims are omitted for readability)

```json
{
  "exp": 1704267723,
  "aud": [
    "cloud-agent",
    "account"
  ],
  "realm_access": {
    "roles": [
      "default-roles-atala-demo",
      "offline_access",
      "uma_authorization"
    ]
  },
  "resource_access": {
    "cloud-agent": {
      "roles": [
        "admin"
      ]
    },
    "account": {
      "roles": [
        "manage-account",
        "manage-account-links",
        "view-profile"
      ]
    }
  }
}
```
The claim is available at `resource_access.<client_id>.roles` by default.
The path to the claim should be configurable by the agent to avoid vendor lock
and remain agnostic to the IAM configuration.

After introducing the role claim, there will be two distinct access control concepts.

  1. Wallet access scope, where the UMA resource defines specific scopes,
     providing fine-grained access to wallet operations.
     For instance, Alice can update a DID but not deactivate a DID on wallet#1.

  2. Agent role, which manages agent-level permissions.
     For example, Alice is an admin for agent #1 and can onboard new tenants,
     but this authority doesn't extend to agent #2.

__Proposed agent role authorization model__

Role is a plain text that defines what level of access a user has on a system.
For the agent, it needs to support 2 roles:

1. __Admin__: `admin`. Admin can never access a tenant wallet.
   Agent auth layer must ignore any UMA permission to the wallet.
2. __Tenant__: `tenant` or implicitly inferred if another role is not specified.
   Tenant must have UMA permission defined to access the wallet.

### Positive Consequences

- Naturally align the boundary of the agent-level role per agent instance
- Ready to use abstraction, minimal configuration to use and include the claim
- Token can be reused across clients, enabling SSO use case
- Keep the wallet access and agent-level role separated

### Negative Consequences

- The `ClientRole` is not part of the standard, other IAM systems may provide different abstraction.
- In some cases, `ClientRole` can be redundant to configure. `RealmRole` may be preferred in those scenarios.

## Pros and Cons of the Options

### Option 2: Use `RealmRole` for defining roles in Keycloak

- Good, because minimal effort is required to define the role and include it in the JWT
- Bad, because roles are at the realm level, making it hard to support some topology

*Note: This option is equally applicable as Option 1, depending on the required topology.*
### Option 3: Use custom user attribute for defining roles in Keycloak

- Bad, because role abstraction is already provided by Keycloak. Engineering effort is spent to reinvent the same concept
- Bad, because it requires more effort to configure the attribute value and map it down to the token

## Links

- [Keycloak ClientRole](https://www.keycloak.org/docs/latest/server_admin/#con-client-roles_server_administration_guide)
