# Hierarchical deterministic key generation algorithm

- Status: accepted
- Deciders: Jesus Diaz Vico, Ezequiel Postan, Pat Losoponkul, Yurii Shynbuiev
- Date: 2023-05-16
- Revision-date: 2024-06-06
- Tags: key management, hierarchical deterministic, key derivation

Technical Story:

Current ADR is based on the Research Spike [Evaluation of Using a Single Mnemonic both for Cryptocurrency and Identity Wallets](https://drive.google.com/file/d/1SRHWRqY1C88eVuaS1v_uIAt-LNTLi9aO/view) document was written by Atala engineers:

- Jesus Diaz Vico (Atala Semantics team)
- Christos KK Loverdos (Atala Technical Director)
- Ezequiel Postan (Atala Semantics team)
- Tony Rose (Atala Head of Product)

Reviewed in 2024 by Atala engineers:
- Jesus Diaz Vico
- Ezequiel Postan
- Pat Losoponkul
- Gonçalo Frade
- Yurii Shynbuiev

The document covers motivation, the overview of BIP32-based HD wallets, and the main concept and implementation details.

## Context and Problem Statement

The Identus Platform must provide the hierarchical deterministic (HD) key derivation algorithm for the identity wallets managed by the platform (Cloud Agent and SDKs)

HD key derivation can be used for both `managed` and `unmanaged` solutions. In both cases, the key material is created from the `seed`.

In the case of a `managed` solution, the keys are created by the `Agent` or `SDK` and stored in the `secured storage` that is managed by the Identus platform.

In the case of an `unmanaged` solution, the key material is created by the tools (for instance, `identus-cli`) following similar rules, and is stored on the client side in the `secret storage` managed by the client.

We are going to be using different derivation implementations for secp256k1 and ed25519.

## Out of the Scope

### `did:peer`

DID peer key material derivation is out of the scope of this ADR, so this type of DIDs is created dynamically from the secure random

### Recovery Procedure

Key material recovery procedure is out of the scope of this ADR, but the main idea is the following: having a `seed` and the latest versions of the DID Documents published on-chain it is possible to recover all the key materials related to the latest state of the identity wallet related to DIDs.

### Implementation details

The HD key derivation algorithm is a part of the Apollo building block, the choice of the programming language is up to the engineering team.

### Secure Storage

Secure store implementation is a matter of another ADR. By now, the Hashicorp Vault is going to be used by the Identus platform by default.

### Backward Compatibility with the PRISM v1.4

The current decision doesn't have backward compatibility with the PRISM v1.4, but it can be mitigated by switching to the `unmanaged` way of key management for the DIDs created in v1.4 or by implementing the backward compatibility module in the Identus Platform


## Decision Drivers

- Deterministic key derivation for the Identus Platform and in all components: Cloud Agent (JVM), Identity Wallets (Android, iOS, Web)
- Possibility to use the same `seed` value for `crypto` and `identity` wallets.
- Compliance with BIP32 specification
- Key material migration between the wallets
- Key material recovery in case of disaster
- We must use different derivation standards bip32 with secp256k1 and ed25519-bip32

## Considered Option

Implement the HD key derivation algorithm according to the research spike for all the components of the Identus Platform.
The derivation path contains the following segments/layers:

m/wallet-purpose'/did-method'/did-index'/key-purpose'/key-index'

`wallet purpose` is used to distinguish the wallet purpose for the identity wallet and is a constant for the Identus platform `0x1D`, which looks like ID

`did-method` - the DID method that the key will be generated for. The value of `did-method` should be registered. The following are available values for the `did-method`:

- PRISM DID method - `0x1d`

`did-index` - the index of the DID, it's possible to create

`key-purpose` - the purpose of the key associated with the DID. There are the following available values for the `key purpose`:

- master key 1 - is the most privileged key type, when any other key is lost, you could use this to recover the others
- issuing key 2 - is used for issuing credentials only, it should be kept in a safe place to avoid malicious credentials being issued.
- key-agreement key 3 - is used to establish a shared symmetric key for secure end-to-end communication, use this key type to encrypt the content.
- authentication key 4 - is used to authenticate requests or log into services.
- revocation key 5 - is used for revoking credentials only, it should be kept in a safe place to avoid malicious credentials being issued.
- capability-invocation key 6 - is used to specify a verification method that might be used by the DID subject to invoke a cryptographic capability, such as the authorization to update the DID Document.
- capability-delegation key 7 - is used to specify a mechanism that might be used by the DID subject to delegate a cryptographic capability to another party, such as delegating the authority to access a specific HTTP API to a subordinate.

`key-index` - the index of the key pair

In order to generate key material (private and public keys):
- Secp256k1 ellipstic curve will be used with standard bip32 derivation
- Curve25519 (Ed25519) will be used with the standard bip32 implementation for [ed25519](https://ieeexplore.ieee.org/document/7966967)
- Future implementations will require their own implementations of the derive function, and very potentially at some point we may want to rework bip32 implementation to make it more agnostic, because a high percentage of the code is going to be the same.

`Seed` entropy must be used for the HD algorithm is 256 bits which corresponds to 24 words mnemonic

## Decision Outcome

The PRIMS platform uses HD key derivation algorithm for `managed` and `unmanaged` solutions based on the research spike the current ADR.

### Positive Consequences

- deterministic key material derivation among all components of the Identus platform
- BIP32 compliance (for both secp256k1 and ed25519 with their corresponding implementations)
- key material migration capability
- key material recovery capability

### Negative Consequences

- backward compatibility with the key material created by PRISM v1.4 version (can be mitigated by removing the `wallet_purpose` from the derivation path)

## Links

- [Evaluation of Using a Single Mnemonic both for Cryptocurrency and Identity Wallets](https://drive.google.com/file/d/1SRHWRqY1C88eVuaS1v_uIAt-LNTLi9aO/view)
