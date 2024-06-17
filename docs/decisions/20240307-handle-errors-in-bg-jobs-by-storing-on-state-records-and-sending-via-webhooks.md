# Handle errors in background jobs by storing on state records and sending via webhooks

- Status: accepted
- Deciders: David Poltorak, Yurii Shynbuiev, Benjamin Voiturier, Fabio Pinheiro
- Date: 2024-03-07
- Tags: error handling, background jobs, DIDComm, messaging

## Context and Problem Statement

In our system, background jobs play a crucial role in handling tasks such as processing DIDComm messages for establishing connections between agents. Currently, if an error occurs in a background job, the original caller is not notified of this error, leading to potential issues in tracking and managing the state of operations. If a ZIO Failure is encountered, a serialised version of the error (string) is stored in the database along the background job record, however, this attribute is not available on the API when checking the status of an operation.

While the DIDComm Error Reporting protocol effectively handles errors in peer-to-peer communications between agents, our system lacks a robust mechanism for notifying clients of errors occurring in background jobs. This gap in error handling affects transparency and the ability to respond to issues promptly. How can we ensure clients are effectively notified of errors in background jobs, especially when such errors cannot be communicated via DIDComm Error Reporting?

## Decision Drivers

- Transparency and traceability of errors
- System reliability and robust error handling, reliability of background job processing
- Minimising the impact of errors on user experience
- Complementing the DIDComm Error Reporting protocol
- Future scalability is not hampered by the solution we put in place

## Considered Options

1. Storing error information in database records
 - Storing in RFC 9457 Problem Details for HTTP APIs format
 - Storing in proprietary format
 - Storing as ZIO.Failure string (as is)
 - Enhancing the API to return this attribute of the record when checking the status of an operation
2. Creating a central registry of errors
3. Using existing webhook system to send errors to clients
4. Implementing event-driven error notifications

## Decision Outcome

Chosen Option: Combined 1 and 3

We have opted for enhanced error handling by storing error details on background job records in the RFC 9457 Problem Details for HTTP APIs format and leveraging webhooks for error notifications. This approach is selected because it aligns with our objectives of enhancing system reliability and error handling for background jobs. It provides a transparent mechanism for users to be informed about errors and leverages the existing webhook system, thereby avoiding the introduction of unnecessary complexity through event-driven architecture or a central registry.

### Implementation Strategy

- Storing Error Information on Job Records: We will serialise error details into a JSON object that adheres to the RFC 9457 structure and store this information in a dedicated field within the background job records. This method is intended to enhance the visibility of errors and assist in the debugging process.
- Include error attribute in API responses: We will ensure that the error object is returned on any object which represents the state of a background job (Connection, Issuance or Present Proof)
- Webhook Notifications: Our strategy includes making use of the existing webhook system to notify clients of errors in real-time. This utilisation ensures that clients are promptly informed of any issues, enhancing the overall user experience and system responsiveness to error conditions.

### Positive Consequences

- Clients receive immediate, standardised notifications of errors, improving transparency.
- Error details are systematically logged, enhancing system monitoring and debugging capabilities.
- The approach scales well with system growth and can adapt to future requirements for error handling.
- **Error messages logged on records will be secured by the active WalletContext**

### Negative Consequences

- No central place to access error instances as per RFC 9457 specification.
- Clients will need to manage potential unknown failures of webhook calls to their system (from agent to client) as webhook events are not persisted.
- Error information will need to be made available when retrieving background job processing records through the operation API that generated them.

### Storing error information in database records

- Good, because it provides a persistent record of errors.
- Good, RFC 9457 Problem Details for HTTP APIs format, as it provides a standardised way to represent errors.
- Bad, proprietary format, as it requires converting to RFC 9457 to be sent to the user.
- Bad, because it requires manual checking and doesn't proactively notify interested parties.

### Creating a central registry of errors

- Good, because it centralises error management.
- Bad, because it can become a bottleneck and still lacks proactive notification.
- Bad, because it carries a lot of complexity that may not get used as data can be made available on other RESTFul APIs in a more cohesive way (such as retrieving a connection record can include the errors about creating that connection).

### Using existing webhook system to send errors to clients

- Good, because it leverages existing implementation and communication channels to send events.
- Bad, because it is limited by the existing system (events not persisted).

### Implementing event-driven error notifications

- Good, because it provides real-time, scalable notifications.
- Bad, because it requires the setup and management of an event-driven system.

## Links

- [DIDComm Messaging Specification](https://identity.foundation/didcomm-messaging/spec/)