# Mercury

## Index

- [Mediator (Mailbox)](./Mercury-Mailbox-Mediator.md)
- Protocols:
  - [Invitation-Protocol](./protocol-invitation/Invitation-Protocol.md)
  - [Mercury-Mailbox-Protocol](./protocol-mercury-mailbox/Mercury-Mailbox-Protocol.md)
  - [Report-Problem-Protocol](protocol-report-problem/Report-Problem-Protocol.md)
  - [Routing-Protocol](./protocol-routing/Routing-Protocol.md)
- [Quick start](./QuickStart.md)
- [UseCases](./UseCases.md)

## Project structure

Dependencies:

```mermaid
flowchart BT
  models
  %%[mercury-data-models]
  resolver
  %%[mercury-resolver]
  invitation[protocol-invitation]
  mailbox[protocol-mercury-mailbox]
  routing[mercury-protocol-routing-2_0]
  agent
  agent-didcommx
  agent-didscala

  alice((Alice))
  bob((Bob))

  subgraph Libs
    didcommx
    did-scala
    http[shttp or zhttp]
  end



  subgraph Mercury
    subgraph Protocols
      invitation
      mailbox
      routing
    end

    subgraph DID agents
      alice
      bob
      agent-cli-didcommx
    end

    resolver --> models
    agent --> resolver

    invitation --> models
    mailbox --> models
    routing --> models
    mailbox --> invitation
    mailbox --> routing

    alice -.->|client| mailbox
    alice --> agent-didcommx
    bob --> agent-didcommx


    agent ---> models
    agent -..-> routing
    %% invitation


    agent-didcommx --> agent
    agent-didcommx -.-> didcommx

    agent-cli-didcommx -.-> http
    agent-cli-didcommx --> agent-didcommx

    agent-didscala --> agent
    agent-didscala -.-> did-scala
  end




```

## Quick Reference Guide

```shell
# Alice Agent (send messagem to Bob's Mediator)
sbt "agentDidcommx/runMain org.hyperledger.identus.AgentClientAlice"

# Bob Agent (fetch his message from Mediator)
sbt "agentDidcommx/runMain org.hyperledger.identus.AgentClientBob"
```
