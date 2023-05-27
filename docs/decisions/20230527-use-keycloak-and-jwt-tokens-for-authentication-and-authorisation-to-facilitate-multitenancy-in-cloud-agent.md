# Use Keycloak and JWT tokens for Authentication and Authorisation to facilitate multitenancy in cloud agent

- Status: accepted
- Deciders: David Poltorak, Yurii Shynbuiev, Shailesh Patil, Ben Voiturier
- Date: 2023-05-27
- Tags: multitenancy, authorisation, authentication

Technical Story: [Research Spike - 1d: find a way to authenticate and authorise the PRISM agent instance administrator | https://input-output.atlassian.net/browse/ATL-4362]

## Context and Problem Statement

Prior to this Architectural Decision Record (ADR) and the related Value Brief, authentication (AuthN) and authorisation (AuthZ) for API consumers of an agent are implemented using a pre-shared key, supplied as an API token within each request header.

An agent can support a single-tenant only.

Each single-tenant agent is accessed via a shared API gateway layer (APISIX) that enforces a consumer restriction list. Only assigned consumers, identified through the pre-shared key, can access specific agent instances.

This authentication/authorisation mechanism poses a significant security risk. If the pre-shared key is leaked, we lack the means to detect its misuse by a nefarious actor, as there is no proof-of-possession mechanism or additional authentication factor in place.

In our Multi-tenant Value Brief, we propose modifications to the agent, enabling it to host multiple tenants within a single instance. Here, a tenant is defined as a unique set of private keys and configurations shared by multiple API consumers.

As we transition to multi-tenancy, several critical questions emerge:

1. How should the cloud agent authenticate, or verify the identities of, its API consumers?
2. How should the cloud agent authorise a particular identity to use a specific instance of PRISM?
3. As the cloud agent becomes capable of hosting multiple tenants whose workloads must remain isolated, how should it become tenant-aware? That is, how should it determine which tenant an API consumer belongs to, and authorise them to manage and operate within that tenant?
4. How can we mitigate the security risk associated with a leaked pre-shared key/token?"

## Decision Drivers

- The complexity of the solution to implement, run and maintain
- Ability to offer solution as SaaS offering as well as self-hosted option
- Use industry standard approaches for frictionless adoption
- Not having to roll our own AuthN/AuthZ implementations [Engineering principle: build differentiating value]
- Ability to effectively mitigate pre-shared key security risk

## Considered Options

All options use OIDC and the Client Credentials Grant flow which is suitable for machine-to-machine use.
 
We have not included an option where we write our own AuthN/AuthZ implementation. All options require an additional component to be added to the stack to store identity related data [Users, roles etc] and to potentially act as a Policy Decision Point (PDP), Policy Administration Point (PAP) and a Policyf Information Point (PIP).

### Keycloak as AuthN/AuthZ

- Keycloak with opaque tokens (without digital signatures)
- Keycloak with JWT tokens (without digital signatures)

### Keycloak as AuthN, another system as AuthZ

- Keycloak with JWT tokens and Open Policy Agent (OPA) (without digital signatures)

### Digital Signatures/Proof of Possession

- Keycloak with any token type with Demonstration of Proof of Possession (DPoP)
- Keycloak with any token type with a custom scheme using Decentralized Identifiers (DIDs)
- Keycloak with any token type using Mutual TLS (mTLS)

## Decision Outcome

Chosen option: "Keycloak with JWT tokens (without digital signatures)", because it provides a balance between security, complexity, and maintainability while using industry-standard approaches, and reduces the need to develop custom AuthN/AuthZ implementations. Application layer can decode JWT and use scope and claims to identify tenant of the consumer.

### Positive Consequences

- Industry standard OAuth2/OIDC is used for authentication, ensuring compatibility with many services.
- Utilizes Keycloak as an Identity Provider (IdP), providing a centralized and robust service for managing identities.
- JWT tokens allow claims and scopes to be directly embedded in the token, which helps in authorization.
- APISIX, acting as the Policy Enforcement Point (PEP), can validate JWT tokens without a round trip to Keycloak.
- Risk of key/token leakage is reduced as compared to pre-shared keys.

### Negative Consequences

- Complexity of JWT token management, including key rotation and revocation.
- Need for a caching and refresh strategy when verifying JWT in the APISIX and application layer.
- Possible increased latency due to JWT verification at both APISIX and application layers.
- Reupidation threat minimised by short OIDC access token lifetime but not fully mitigated as no digital signature implemented.

## Pros and Cons of the Options

### The use of Keycloak in general

- Good, becasue APISIX and Keycloak are easy to integrate with well documented plugins.
- Bad, because of the need to run Keycloak [compute resources and management overhead].

### Keycloak with opaque tokens (without digital signatures)

*Keycloak is utilized for authentication, whereas authorisation requires APISIX and the application layer to make a call to Keycloak. This is because the opaque token, which cannot be decoded outside of Keycloak, doesn't contain any permission-related information, necessitating the authorisation check.*

- Good, because it simplifies token management.
- Good, because tokens are not self-contained and therefore don't expose any information.
- Bad, because it requires a round trip to Keycloak to validate each token and perform authorisation checks, increasing latency.

### Keycloak with JWT tokens (without digital signatures)

*Keycloak is utilized for authentication, while authorisation is handled by APISIX and the application layer. Both the APISIX and application layer need to call Keycloak's JSON Web Key Set (JWKS) endpoint to retrieve public keys to decode and validate JWTs. However, the actual authorisation process is handled internally, leveraging data added to JWTs as part of scope and claims. This approach reduces latency compared to the authorisation checks required for opaque tokens.*

- Good, because JWT tokens can be validated by APISIX without a round trip to Keycloak.
- Good, because claims and scopes can be embedded directly in the token.
- Bad, because it introduces complexity around JWT management, including key rotation and revocation.

### Keycloak with JWT tokens and Open Policy Agent (OPA) (without digital signatures)

*Keycloak is utilized for authentication, while APISIX and the application layer make a call to an OPA service for authorisation. Additionally, they need to contact Keycloak's JWKS endpoint to retrieve public keys, enabling them to decode and validate JWTs. Authorisation policies are articulated using the powerful OPA language.*

- Good, because it provides a powerful and flexible approach to authorisation.
- Good, because it works well with JWT tokens, enabling authorization checks to be performed based on JWT claims.
- Bad, because it introduces additional complexity and another component to maintain (in addition to Keycloak).

### Keycloak with any token type with DPoP

*Only works in oAuth2/OIDC flow

- Good, because DPoP provides a method for binding access tokens to a particular client.
- Good, because it enhances the security by reducing the threat of token theft.
- Bad, because it introduces additional complexity around token management.

### Keycloak with any token type with a custom scheme using DIDs

- Good, because DIDs provide a self-sovereign method of identity verification.
- Good, because it enhances security by ensuring that only the valid owner of a DID can authenticate.
- Bad, because it adds a considerable amount of complexity to token management, and DIDs are still relatively new and may not be widely adopted or fully standardized.

### Keycloak with any token type using Mutual TLS (mTLS)

- Good, because it provides a strong method of security by requiring both client and server to authenticate each other.
- Good, because it mitigates repudiation threats.
- Bad, because it introduces complexity around certificate management and may add additional overhead in terms of performance.

## Links

- [Keycloak documentation](https://www.keycloak.org/docs/latest/)
- [APISIX documentation](https://apisix.apache.org/docs/)
- [Open Policy Agent (OPA) documentation](https://www.openpolicyagent.org/docs/)
- [JWT (JSON Web Tokens) Introduction](https://jwt.io/introduction/)
- [OAuth 2.0 documentation](https://oauth.net/2/)
- [Information on OAuth 2.0 Token Binding - DPoP](https://tools.ietf.org/id/draft-ietf-oauth-dpop-03.html)
- [Decentralized Identifiers (DIDs) documentation](https://www.w3.org/TR/did-core/)
- [Overview of Mutual TLS (mTLS)](https://www.cloudflare.com/learning/ssl/what-is-mutual-tls/)
- [JWT vs Opaque Tokens](https://zitadel.com/blog/jwt-vs-opaque-tokens)

