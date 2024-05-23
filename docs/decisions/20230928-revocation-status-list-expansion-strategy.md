# JWT credential revocation status list expansion strategy

- Status: accepted
- Decider: Benjamin Voiturier, Yurii Shynbuiev, Ezequiel Postan, Shota Jolbordi
- Date 2023-09-28
- Tags: revocation, revocation-status-list, jwt-vs-revocation, statusList2021

Technical Story: [Revocation status list expansion strategy decision | https://input-output.atlassian.net/browse/ATL-6012]

## Context and Problem Statement

In the process of devising a mechanism for revoking JWT credentials, we've opted to implement the [statusList2021](https://www.w3.org/TR/vc-status-list/) method.
This approach entails the creation of a "status list credential" that incorporates a gZip compressed status list in a form a bitString.
The specification recommends a minimum size of 16 KB for the status list included in a credential.
However, it does not delineate a maximum size, nor does it provide guidance on how to proceed if the selected status list surpasses its capacity to store information about revoked credentials.
Put differently, if more credentials are issued than can be accommodated by a 16 KB status list, no specific instructions are provided.


## Decision Drivers

We must determine a strategy for expanding the revocation status list to accommodate the increasing number of revoked credentials in the future.

It's crucial to keep in mind that this status list will be part of a "status list credential."
This credential will be frequently requested through the REST API during verification by verifiers and will be downloaded over the network.
Therefore, we need to ensure that the status list remains reasonably small in size to prevent any slowdowns in the verification process.

In the future, there might be a need to reorganize the state and possibly move status lists to another public registry for verifiers to depend on. This is not the current scenario, as each Cloud Agent currently maintains status lists specific to their respective tenants.

Absolutely, it's crucial to avoid overengineering the solution. This ensures that the code remains manageable and easy to maintain in the long run.


## Considered Options


Option 1: Increment status list size as we approach its limit:

We'll enhance the status list by simply doubling its size.
If we opt for the minimum size of 16 KB for the revocation status list, it can manage up to 131,072 revocable credentials before requiring expansion.
It's important to note that this number represents the potential capacity for revocable credentials to be issued, not necessarily the actual number of credentials that have been revoked.
Even unrevoked credentials still occupy space.

Option 2: Generate multiple revocation status lists as the previous one reaches its limit

With this approach, we'll generate and store multiple status list credentials.
It will be crucial to ensure that each credential is linked to a specific status list, allowing us to track where the revocation information is stored.
If we stick with the smallest recommended status list size, one revocation status list can hold information about 131,072 revocable credentials.


## Pros and Cons of the Options


#### Option 1
Option 1 offers the primary advantage of being straightforward to implement.
It is also important to note that Option 2 isn't significantly more challenging to implement, so we shouldn't overly prioritize this consideration.

One potential drawback of Option 1 is that the size of the status list could potentially become too large, leading to slower verification due to the increased payload of the verification status list credential.
However, it's worth noting that the verification status list credential is gZipped.
This means that consecutive 0s and 1s will be compressed.
For example, a sequence of 5 zeros (00000) will be stored as 5(0), indicating five consecutive zeros.
Assuming that most credentials won't be revoked and will have an index of 0 in the status list, the gzipped status list in the status list credential should be very compact.
This is the most crucial factor to consider in the end.

#### Option 2

Option 2 slightly reduces privacy compared to Option 1 in certain scenarios.
For example, in cases where the number of Verifiable Credentials (VCs) starts small but grows over time.
Initially, both options face the same issue with a small anonymity set due to the limited number of VCs issued.
As the number of VCs increases, Option 1 maintains a continuously growing anonymity set.
However, in Option 2, when the issuer reaches the 16KB limit and creates a new list, there will be a period where the new list has only a few VCs, resulting in a smaller anonymity set for VCs in the second list.


Option 2 however, has a big advantage considering upcoming need for AnonCreds revocation.
AnonCreds doesn't allow for expanding the status list size once defined during revocation registry creation.
Pushing back Option 2 for AnonCreds and starting with an initial capacity of 1 million credentials may not be efficient.
The size of the attached TAILS FILE grows rapidly with capacity (e.g., 8.4MB for 32,768 VCs!).
This file needs to be resolved/downloaded by the holder during the issuance process.


## Decision Outcome

Given that the implementation of Option 2 is not significantly more complicated than Option 1, and considering that JWT credentials, specifically statusList2021, are not inherently private, we have decided to proceed with Option 2.
This choice is more future-proof, especially in light of the anticipated need to implement AnonCreds revocation in the future.

