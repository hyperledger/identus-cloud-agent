# Storage for SSI related resources 

- Status: accepted 
- Deciders: Javi, Ben, Yurii 
- Date: 2024-05-20
- Tags: Verifiable Data Registry (VDR), decentralized storage


## Context and Problem Statement

The main question to answer is: What is the most practical way to store resources related to VC verification and revocation?

In the context of SSI, there are resources such as credential definitions, schemas, revocation lists, etc., that are referenced inside VCs. These resources need to be accessible to different parties in order to verifiy the credentials. In this ADR, we discuss the trade-offs of different storage alternatives.

## Decision Drivers

A desired solution should balance

- Methods for data integrity and authenticity validation: For instance, if we are referring to a credential definition, the user retrieving the resource should be able to validate that the resource hasn't been altered since its creation. In the case of more dynamic resources, such as revocation lists, which are actually updated throughout time, the recipient party would need to validate that the resource was created by the issuer.
- Data availability: It is important for resources to be highly available. If a resource is missing, it can lead to an inability to validate a VC.
- Decentralization: There must be a consideration to avoid innecesary central points of failure
- Historical data requests: Some use cases may require support for querying historical data. For example, retreive a revocation list at certain point in the past.
- Write access control: Most generally issuers (as they create most of the resources), require to have control of the data they store in order to be able to update it when needed, and also to avoid third parties to make un-authorized changes.
- Latency, throughput, deployment costs: Any solution should provide a reasonable balance of non functional requirements, such as achieving enough throughput, or having low enough latency for the system to be practical.

## Considered Options

We considered the following alternatives, which contemplate the approaches currently discussed by the broad SSI ecosystem at the time of this writing.

- URLs and traditional HTTP servers: with no surprises, in this approach, each resource is identified with a URL and stored in traditional servers. The URLs will encode hashes as query parameters to enforce integrity for static resources. Dynamic resources will be signed by the resource creator's key.
- DID URLs and traditional HTTP servers: in this variation, resources are still stored in servers. Resources are identified by DID URLs that dereference services of the associated DID document. The services will contain the final URL to retrieve the corresponding resources. Once again, hashes will be associated to static resources as DID URL query parameters, while dynamic resources will be signed adequately. 
- IPFS: An IPFS approach would be useful for storing static resources using IPFS identifiers for them. Dynamic resources however become more challenges. Even though we recognize the existence of constructions like IPNS or other layers to manage dynamic resources, we find them less secure in terms of availability and consistency guarantees.
- Ledger based storage (Cardano in particular): In this approach, resources would be stored in transactions' metadata posted on-chain. The data availability and integrity can be inherited from the underlying ledger. 
- A combination of previous methods and the use of a ledger: Similar as above, data references are posted on-chain, but the actual resources are stores in servers. The servers could be traditional HTTP servers or IPFS nodes.

## Decision Outcome

After a careful analysis we concluded the following points:
- There is an architectural need to develop a "proxy" component, a.k.a. VDR proxy, that would work as a first phase for resource resolution. Behind the VDR proxy, different storage implementations could be added as extensions
- With respect to specific implementations
  + ledger based storage at this stage introduces latency, throughput bottlenecks, costs and other issues not suitable for most use cases. 
  + Hybrid solutions that make use of a ledger share similar drawbacks. 
  + Decentralized Hash Tables (such as IPFS) do not provide efficient handling for dynamic resources (such as revocation lists).
  + We concluded that a reasonable first iteration could be delivered using DID URLs to identify resources while they would be, a priori, stored in traditional HTTP servers.

### Positive Consequences

- The implementation of a VDR proxy enables a transparent abstraction that allows to extend the possible methods to retrieve resources.
- DID URLs allow for a fair decentralization level at issuer's disposal to control the location of resources

### Negative Consequences

- There is a level of under-specification at W3C specifications with respect to DID URL dereferencing. This forces us to define the under-specificied behaviour or simply creata-our-own solution.

## Links 

We leave a list of useful links for context

- [AnonCreds Methods Registry](https://hyperledger.github.io/anoncreds-methods-registry/)
- [AnonCreds Specification](https://hyperledger.github.io/anoncreds-spec/)
- [W3C DID resolution algorithm](https://w3c-ccg.github.io/did-resolution/)
 
