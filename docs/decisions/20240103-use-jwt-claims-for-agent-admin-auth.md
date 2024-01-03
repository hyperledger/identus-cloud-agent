# Use JWT claims for agent admin access control

- Status: accepted
- Deciders: Pat Losoponkul, Yurii Shynbuiev, David Poltorak, Shailesh Patil
- Date: 2024-01-03
- Tags: multitenancy, authorisation, authentication

Technical Story: [Allow agent admin role to be authenticated by Keycloak JWT | https://input-output.atlassian.net/browse/ATL-6074]

## Context and Problem Statement

External IAM authentication and authorization are currently limited to tenants using the wallets.
Administrators currently rely on a static API key configured in the agent.
Enhancing the security of admin access requires extending support for admin authentication through an external IAM.
This introduces a concept of role to the authentication and authorization process.

The existing tenant authentication and authorization model relies on UMA (user-managed access)
which defines resources, policies, and permissions.
Tenants gain access to resources based on defined permissions, effectively controlling resource access.
In addition to wallet usage, the agent also handles wallet management,
a functionality utilized by both tenants and administrators.
While administrators don't directly use the wallet, they oversee its management.
Integrating an auth model to distinguish between admins and tenants presents a new challenge.

- Where and how to define the role of admin and tenant?
- What boundary the admin role should be scoped to?
- How to support different deployment topologies? (e.g. multi-agent in a shared realm, but not share the admin users)
- How does it interact with the resource scope in the UMA model?

## Decision Drivers

- Must not prevent us from using other IAM systems
- Must not prevent us from supporting fine-grained tenant wallet access in the future
- Must allow SSO configuration
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

4. Reuse UMA model and define the agent as a resource with the resource scope as a role

    In this option, the agent is defined as another UMA resource and the user role is defined by the agent resource scope.

## Decision Outcome

Option 1: Use `ClientRole` for defining roles in keycloak.

This option limits the admin role of the agent to the client level,
aligning the admin scope with the client, as admin access naturally belongs to an agent instance.
It enables fine-grained role per agent instance, allowing multiple agent instances to a shared realm.

In this configuration, a `ClientRole` called `agent-admin` must be created for each client.
Users can be mapped to the `ClientRole` using groups, attributes, or other metadata according to the use case.
The role is automatically included in the JWT claim by default, and they are segregated per client.
This enables multiple agent instances to reuse the same token while having their own set of roles defined.

Example JWT payload containing `ClientRole`. (Some claims are omitted for readability)

```json
{
  "exp": 1704267723,
  "aud": [
    "prism-agent",
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
    "prism-agent": {
      "roles": [
        "agent-admin"
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
This is only a convention, not the standard.
The path to the claim should be configurable by the agent to avoid vendor lock.
The agent checks the token to see if it contains the role `agent-admin`,
then allows the admin-related operations to be performed.

After introducing the role claim, there will be two distinct access control concepts.

  1. Wallet access scope, where the UMA resource defines specific scopes,
     providing fine-grained access to wallet operations.
     For instance, Alice can update a DID but not deactivate a DID on wallet#1.

  2. Agent role, which manages agent-level permissions.
     For example, Alice is an admin for agent #1 and can onboard new tenants,
     but this authority doesn't extend to agent #2.

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

- Good, because minimal effort is required to define role and include it in the JWT
- Bad, because roles are at realm level, making it hard to support some topology

### Option 3: Use custom user attribute for defining roles in Keycloak

- Bad, because role abstraction is already provided by Keycloak. Engineering effort is spent to reinvent the same concept
- Bad, because it requires more effort to configure the attribute value and map it down to the token

### Option 4: Reuse UMA model and define the agent as a resource with the resource scope as a role

- Good, because UMA resource access is already supported by the agent
- Bad, because the agent itself is not a resource but a resource server. While it could work, it is unnatural to maintain.
- Bad, because the wallet resource is mixed with the agent resource
- Bad, because the wallet access scope is mixed with the agent access scope

## Links

- [Keycloak ClientRole](https://www.keycloak.org/docs/latest/server_admin/#con-client-roles_server_administration_guide)
