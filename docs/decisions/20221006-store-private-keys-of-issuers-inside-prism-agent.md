# Store private keys of Issuers inside prism-agent

- Status: accepted
- Deciders: Benjamin Voiturier, Pat Losoponkul, Miloš Džepina, Shailesh Patil, Shota Jolbordi, Bart Suichies, Ezequiel Postan, Yurii Shynbuiev, David Poltorak
- Date: 2022-10-05

## Context and Problem Statement

While each holder has a wallet application on the phone (edge agent) to store private keys, contacts, and credentials, PRISM 2.0 will provide a custodial solution to Issuers and Verifiers. Thus they won't have their wallets or store/manage keys. There needs to be storage for the private keys of issuers and Verifiers on the PRISM side.


## Considered Options

- Having issuers store and manage their own keys on the edge wallet (prism 1.4 approach)
- Storing keys in a dedicated wallet application that is connected to prism-agent (cloud agent)
- Having prism-agent store and manage keys directly 


## Decision Outcome

Chosen option: Option 3, because it is the simplest approach that satisfies the needs of providing the Issuer and Verifier with key storage while also not requiring them to manage their own keys. Option 3 was chosen instead of Option 2 because it achieves the same goal but does not require work on integrating another wallet application, so in short, it is simpler and faster to implement.     

### Negative Consequences <!-- optional -->

While Option 3 is simpler to implement then Option 2 and provides basic functionality required to solve the problem emphasized in [ Context and Problem Statement](#context-and-problem-statement), it does not provide full functionality and security of widely used and well tested wallet application. Therefore this decision is considered to be temporary and made only in the interest of solving the problem as fast as possible. 


## Links

- [Recording of the meeting where decision was made](https://drive.google.com/file/d/120YyW2IEpl-F-6kF0V0Fau4bM7BbQ6mT/view?usp=sharing) 
