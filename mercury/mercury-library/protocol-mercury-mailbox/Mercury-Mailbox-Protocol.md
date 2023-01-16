# [WIP] Mercury Mailbox Protocol

## PIURI

TODO maybe ??? `https://atalaprism.io/mercury/mailbox/1.0`

## Diagrams (Mailbox Operations)

### Flow Diagram

```mermaid
sequenceDiagram
  participant Alice
  participant Mediator
  participant DID Resolver

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
```

### Service State Machine - Mediator POV

```mermaid
stateDiagram-v2
  state "Idle" as idle
  [*] --> idle
  idle --> [*]
  idle --> recieved_forward_message                     :New msg type forward
  recieved_forward_message --> idle                     :Invalid Request
  recieved_forward_message --> *process_forward_message :Valid Request
  *process_forward_message -->  idle
  idle --> recieved_mailbox_operation                      :New msg type mailbox operation
  recieved_mailbox_operation --> idle                      :Invalid Request
  recieved_mailbox_operation --> *process_mailbox_operation   :Valid Request
  *process_mailbox_operation --> idle
```

```mermaid
stateDiagram-v2
  [*] --> process_forward_message
  process_forward_message --> storeMessage
  storeMessage --> notify_recipient            :Message stored
  notify_recipient --> notify_recipient        :retry
  notify_recipient --> [*]                     :acknowledge (or get \n a pickup operation)
  notify_recipient --> [*]                     :timeout \n (like after 1 year)
```

```mermaid
stateDiagram-v2
  [*] --> process_mailbox_operation
  process_mailbox_operation --> send_challenge
  send_challenge --> [*]                     :timeout (like \n 10min session)
  send_challenge --> *operation              :challenge response
  *operation  --> [*]
```

NOTE: message_sent is just a operation type!!

FIXME replace "pickup" with "operation"

TODO: the last diagram is maybe its on protocol.
(Does a challenge protocol already exists) ???

---

### Service State Machine - Alice Agent POV

**Pickup Operation:**

```mermaid
stateDiagram-v2
[*] --> messages_requested                          :Requested pending messages pickup
messages_requested --> message_replyed              :Recived message solve the Challenge
message_replyed -->  recieve_messages               :recived the messages
message_replyed --> messages_requested              :Retry with new challenge
recieve_messages --> messages_requested             :Request more messages
recieve_messages --> [*]
```
