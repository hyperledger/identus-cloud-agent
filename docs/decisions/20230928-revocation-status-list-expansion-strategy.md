# Double revocation status list in size as number of revocable credentials exceeds minimum size of the status list 

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

In the future, there might be a need to reorganize the state and possibly merge various status lists into a single comprehensive registry for verifiers to depend on. This is not the current scenario, as each Prism agent currently maintains status lists specific to their respective tenants.

Absolutely, it's crucial to avoid overengineering the solution. This ensures that the code remains manageable and easy to maintain in the long run.


## Considered Options


## Decision Outcome


### Positive Consequences


### Negative Consequences


## Pros and Cons of the Options


## Links

