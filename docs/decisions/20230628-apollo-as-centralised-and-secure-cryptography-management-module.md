# Apollo as centralised and secure cryptography management module

- Status: accepted
- Deciders: Yurii Shynbuiev, Javier Ribó, Gonçalo Frade, Bart, Ahmed Mousa
- Date: 2023-06-28
- Tags: apollo, cryptography

Technical Story: [Apollo Cryptographic Module KMM | https://input-output.atlassian.net/browse/ATL-5006]

## Context and Problem Statement

<br><br>

### 1. Summary
This proposal sets out to crystallize a long-term plan for PRISM's cryptographic functionality. Rather than constructing an entirely new cryptographic functionality, our focus is on integrating robust, secure and tested libraries, meeting several key requirements in the process. 

By leveraging the flexibility of Kotlin Multiplatform, this library will ensure strong, provable security, centralized management of all cryptography, easy upgrades, and efficient code reuse across multiple platforms. 

A significant additional advantage of our chosen framework, particularly for the JavaScript version of this library, is the future potential to export to WebAssembly (WASM).

<br><br>

### 2. Introduction
This proposal outlines a comprehensive plan to develop a cryptographic library using Kotlin Multiplatform. This library will meet our defined requirements and strategically position us for future technological advancements.

#### 2.1 Provable Security
Our cryptographic library will provide engineers with high assurances of security. This will be accomplished by using cryptographic primitives that are secure, with this security being provable through rigorous mathematical proofs. Documentation will accompany these proofs to offer transparency and enable a deeper understanding of the underlying logic and assurances.

#### 2.2 Centralized Cryptography Management
We propose the creation of a cryptographic library that serves as the central management hub for all cryptographic operations within PRISM. By preventing "DIY" implementations, we decrease potential vulnerabilities and establish a standard, thus enhancing overall security across our organization.

#### 2.3 Easy Upgrade Path
In light of emerging cryptographic needs such as the introduction of quantum-resistant cryptography, our library will be designed with easy upgrades in mind. Its modular design will allow for the seamless introduction of new cryptographic primitives as they become necessary or advisable. This adaptability will ensure that cryptographic upgrades across all of PRISM are consistent and efficient.

#### 2.4 Code Reusability
Our library will make the most of Kotlin Multiplatform's capabilities for code reuse across different platforms. We aim to design cryptographic functions that promote this potential, thus minimizing the development effort required for adding new functionality or adapting to different platforms.

<br><br>

### 3. Advantages of Kotlin Multiplatform
Choosing Kotlin Multiplatform for this project affords us several advantages, notably its potential to export to WASM. Not only this stack, but it significantly enhances the utility and versatility of our library, especially for our JavaScript version. While other languages like Rust offer similar capabilities, the use of Kotlin Multiplatform aligns more closely with our resource allocation and current technological strategy.

<br><br>

### 4. Trade Offs
The trade-off gap analysis does not come from the debate between having 1 single language (agnostic) or multiple native platform implementations as this discussion could end super quickly by just reading the  4 points in the Introduction section (Easy upgrade path, code reusability).

The real debate is between choosing the right language that suits us best. We have been analyzing the potential use of Rust or KMM to build the Apollo module.

#### 4.1 Advantages of KMM
Easier to have same interface for the cryptographic functionalities in all platforms
Single Unit test suit to verify platform compatibility between platforms
Version lock for supported library versions
Less pron to compatibility errors between platforms
When a new Platform is added to KMM we can easily test and verify with our code quality standards, if it passes we can add a new platform support

#### 4.2 Disadvantages of KMM
KMM is very powerful but still has some issues the more complex the project is, in Apollo it should be quite straightforward
High dependency on a single point of failure (This can be advantage and disadvantage)

#### 4.3 Advantage of KMM against rust
KMM in this situation has great advantageous against Rust
It might be more difficult to find libraries in Rust that can do all we require for all the platforms.
Testing would not be so straightforward and By platform. It would instead run only on the platform that is building the code.
Uniffi doesnt work for all platforms we provide, so some wrappers would have to be written by hand with C bindings (Not safe and very error pron)

<br><br>

### 5. Implementation Details
We have established several key requirements for our team:

1. Ownership of Apollo and its roadmap is clearly defined. If any issues, doubts, or concerns arise or if any decisions need to be made about the roadmap, tech debt, or any other related aspects, stakeholders know who to contact.

2. We aim to rebuild the current implementations into a more robust system rather than re-using old code from 1.4.

3. Anyone inquiring about the features or algorithms we support will have a clear and accessible resource to answer their questions. We plan to provide a complete list of features that Apollo offers, document them, and make this information accessible to everyone.

4. Apollo will fully document its public APIs in terms of cryptography, compliance, supported algorithms, and key curves.

5. While TypeScript could temporarily survive without WASM, we would significantly benefit from offering native performance for cryptographic libraries through WASM for browsers.

6. Apollo will be tested using best practices, and over time we aim to raise the minimum test coverage threshold. Best practices and quality assurance will be addressed in a collaborative way between Cryptography and engineering, Cryptography engineers will be in charge of making sure our security is robust enough and properly tested and potentially helping us drive unit testing better.

<br><br>

### 6. Definition of Done
In order to consider this completed or done the existing SDK's must have integrated with this new Module.

<br><br>

### 7. Implementation plan

1. Initial Phase: We will establish clear ownership of Apollo and announce it to the rest of the team. From this point forward, the SDK team will take charge of these tasks. This step will facilitate decision-making and clarify who to contact regarding various issues and understanding the roadmap.

2. Phase 1: We will implement wrappers around basic and required functionality for secp256k1, ed25519, and x25519 (generating keyPairs, signing and verifying). We aim to standardize the interface and ensure that it exposes all the functionality that the SDKs need to work.

3. Implementation phase: Phase1 will be by then fully documented, tested and available for other engineers to pick up on their platforms. Engineers will then replace that functionality that was done by third party packages in swift, kmm and typescript with the new Apollo package.

<br><br>

#### Implementation resources
| Engineer | Role | Availability |
| ----------- | ----------- | ----------- |
| Francisco Javier Ribó | Engineering Lead + Developer | Part time |
| Yurii | Engineering + CoreDID Integration Lead | Part time |
| Gonçalo Frade | SDK Project Lead + Roadmap Lead + Developer | Part time |
| Alexandros Zacharakis Jesus Diaz Vico | Cryptography Engineers + Roadmap Lead | Part time |
| Ahmed Moussa | KMM Lead / architect + Developer | Fulltime |
| Cristian Gonzalez | Developer | Fulltime |
| Curtis HArding | Developer | Part time |

<br><br>

### 8. Triage & future roadmap
The main goal of this section is to describe the process where we choose what comes next in Apollo and how we take those decisions.

**Comments**


1. There is a risk of starting to add to Apollo "anything that looks like cryptography". For instance, the Anoncreds part that takes care of formatting the credentials (which is what anoncreds-rs does) should not go into Apollo. 
2. But the underlying cryptographic functionality (for which anoncreds-rs calls libursa) should go into Apollo.
3. Maybe something similar applies to HD wallets.

Owners of this triage process are the engineering + roadmap leads. Those will take decisions based on what has been defined here:

1. Provable Security
2. Centralized Cryptography Management
3. Easy Upgrade Path
4. Code Reusability

<br><br>

### 9. Conclusion

This proposed Kotlin Multiplatform cryptographic library will ensure that PRISM remains at the forefront of secure digital operations by providing strong, provable security, centralized cryptographic management, easy upgradeability, and efficient code reuse. By addressing these critical constraints and harnessing the benefits of Kotlin Multiplatform, we are set to create a library that will set a new standard for cryptographic operations within PRISM.
