# [WIP] Mercury Mailbox Protocol

## PIURI

TODO maybe ??? `https://atalaprism.io/mercury/mailbox/1.0`

## Diagrams (Mailbox Enrollment)

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
idle --> recieved_forward_message
recieved_forward_message --> validation             :Store Message
validation --> idle                                 :Failed validation delete message
validation --> notify_recipient                     :New message
notify_recipient --> [*]
idle --> send_challenge                             :Recived Message pickup       
send_challenge --> message_sent                     :Validate Challenge response
message_sent --> [*]                             
```

### Service State Machine - Alice Agent POV
```mermaid
stateDiagram-v2
state "Idle" as idle
idle --> messages_requested                         :Requested pending messages pickup       
messages_requested --> message_replyed              :Recived message solve the Challenge
message_replyed -->  recieve_messages               :recived the messages 
message_replyed --> messages_requested              :Retry with new challenge
recieve_messages --> messages_requested             :Request more messages 
recieve_messages --> complete                       :send Ack
```