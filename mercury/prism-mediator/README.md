# Mercury

## Index

- [Mediator (Mailbox)](./mediator/Mercury-Mailbox-Mediator.md)
- Protocols:
  - [Invitation-Protocol](./protocol-invitation/Invitation-Protocol.md)
  - [Mercury-Mailbox-Protocol](./protocol-mercury-mailbox/Mercury-Mailbox-Protocol.md)
  - [Routing-Protocol](./protocol-routing/Routing-Protocol.md)
- [Quick start](./QuickStart.md)
- [UseCases](./UseCases.md)

## Project structure

Dependencies:

```mermaid
flowchart BT
  models %%[mercury-data-models]
  resolver %%[mercury-resolver]
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
      mediator
    end

    resolver --> models
    agent --> resolver

    invitation --> models
    mailbox --> models
    routing --> models
    mailbox --> invitation
    mailbox --> routing


    mediator -.->|server| mailbox
    %%mediator --> resolver
    mediator -.-> http

    agent ---> models
    agent -..-> invitation

    agent-didscala
    agent-didscala --> agent
    agent-didscala -.-> did-scala

    agent-didcommx --> agent
    agent-didcommx -.-> didcommx


    mediator --> agent-didcommx


    alice -.->|client| mailbox
    alice --> agent-didcommx
    bob --> agent-didcommx
  end



```

## Quick Reference Guide

```shell
# Mailbox Mediator (zhttp)
sbt "mediator/runMain io.iohk.atala.mercury.mediator.ZhttpMediator"

# Mailbox Mediator (akka)
sbt "mediator/runMain io.iohk.atala.mercury.mediator.Mediator"

# Alice Agent (send messagem to Bob's Mediator)
sbt "agentDidcommx/runMain io.iohk.atala.AgentClientAlice"

# Bob Agent (fetch his message from Mediator)
sbt "agentDidcommx/runMain io.iohk.atala.AgentClientBob"
```
