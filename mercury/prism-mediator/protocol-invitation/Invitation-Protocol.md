# [WIP] Invitation Protocol

This Protocol is parte of the DIDComm Messaging Specification.

Its a out-of-band style protocol.

See [https://identity.foundation/didcomm-messaging/spec/#invitation]

## PIURI

`https://didcomm.org/out-of-band/2.0/invitation`

## Diagrams (Accept Invitation)

FIXME: Strip the diagrams of logic not included on Invitation Protocol!!!

### Flow Diagram

```mermaid
sequenceDiagram
  participant Alice
  participant Mediator
  participant DID Resolver
%%  participant Bob

  note over Alice: Mediation client or Mediated Agent

  rect rgb(0, 120, 255)
    note right of Alice: Alice accepts the invitation and register for a mailbox.
    Alice->>+Mediator: Register (HTTP)
    Mediator->>+DID Resolver: Ask for Alice DID document
    DID Resolver-->>-Mediator: DID document 
    note over Mediator: Confirm the identity
    Mediator-->>-Alice: Registration done
    note over Alice: Alice updates his DID document (adding serviceEndpoint)
  end

%%  note over Bob: Mediation Request
%%  rect rgb(200, 150, 255)
%%    note left of Bob: Bob send a messagem to Alice.
%%    Bob->>+DID Resolver: Ask for Alice DID document
%%    DID Resolver-->>-Bob: DID document 
%%    note over Bob: Find the serviceEndpoint of Alice
%%    Bob->>+Mediator: Forward {Message to Alice} (via HTTP)
%%
%%    opt Depending on the registration (config)
%%      Mediator->>-Alice: Notify or send the playload to Alice 
%%      note over Mediator,Alice: This can be an Email, Webhook, Push API, WebSocket (that is already open), etc
%%    end
%%  end
%%
%%  rect rgb(200, 150, 255)
%%    note right of Alice: Alice read Message.
%%    Alice->>+Mediator: Get message
%%    %% opt 
%%    %%   Mediator->>+DID Resolver: Ask for Alice DID document
%%    %%   DID Resolver-->>-Mediator: DID document
%%    %% end
%%    %% note over Mediator: Confirm the identity
%%    Mediator-->>-Alice: Message to Alice (the playload of the Forward Message)
%%  end
```

---

### Client State Machine - Alice POV

After reading the out-of-band invitation from the Mediator

```mermaid
stateDiagram-v2
    [*]           --> Preparation1  : Processing Invitation
    Preparation1  --> Preparation2  : Create Reply Messagem
    Preparation2  --> Connecting    : Connect
    Connecting    --> Connecting    : Reconnecting
    Connecting    --> [*]           : Giveup (timeout)
    Connecting    --> Enrolling     : Send Reply Messagem
    Enrolling     --> Connecting    : Reconnecting
    Enrolling     --> Enrolling     : Timeout - Resend Messagem
    Enrolling     --> Registered    : Positive response
    Enrolling     --> [*]           : Giveup (timeout)
    Enrolling     --> NotRegistered : Negative Response
    NotRegistered --> [*]           : Negative Response (Giveup)
    NotRegistered --> Preparation1   : Negative Response (Retry)
    Registered    --> Checking      : Update my DID document
    Checking      --> Checking      : Fail or Timeout
    Checking      --> [*]           : Confirmation
    Checking      --> [*]           : Giveup
```

---

### Service State Machine - Mediator POV

```mermaid
stateDiagram-v2
    state "Idle" as idle
    state "
      Received Message
      (Accepting Invitation)
    " as received_message
    state "Validation \n  Check Message validity " as validation
    state "Enrollment Process-1" as enrollment_process1
    % Check if is alredy Registered
    state "Enrollment Process-2" as enrollment_process2

    [*] --> idle
    idle                --> idle                : Waiting for new conections
    idle                --> received_message    : New Conection
    received_message    --> idle                : Negative Response
    received_message    --> validation          : Find DID Document \n (DID revolver)
    validation          --> idle                : Fail
    validation          --> enrollment_process1 : Meet enrollment conditions
    enrollment_process1 --> enrollment_process2 : Create a persistent storage entry for the user
    enrollment_process1 --> reply : Was already enrolled
    enrollment_process2 --> reply 
    reply --> idle

    
```
