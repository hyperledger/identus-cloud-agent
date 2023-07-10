# Webhook Notifications

## Introduction

Welcome to the tutorial on webhook notifications in PRISM Agent. In this tutorial, we will explore how webhook
notifications can enhance your experience with PRISM Agent by providing real-time updates on events. By leveraging
webhook notifications, you can stay informed about important changes happening within the agent.

## Understanding Webhook Notifications

### What are Webhooks?

Webhooks enable real-time communication between applications by sending HTTP requests containing event data to specified
endpoints (webhook URLs) when events occur. They establish a direct communication channel, allowing applications to
receive instant updates and respond in a timely manner, promoting efficient integration between event-driven
systems.

### Purpose of Webhook Notifications in PRISM Agent

Webhook notifications in PRISM Agent serve as a vital feature, enabling you to receive timely updates on various events
occurring within the agent. Webhooks allow you to receive HTTP requests containing event details at a specified
endpoint (webhook URL). These events are specifically related to the execution of
the [Connect](../connections/connection.md), [Issue](../credentials/issue.md),
and [Presentation](../credentials/present-proof.md) flows. Webhook notifications will be sent each time there is a state
change during the execution of these protocols.

By leveraging webhooks, you can integrate PRISM Agent seamlessly into your applications and systems. You can track and
monitor the progress of the main flows, receiving timely updates about changes and events.

## Configuring the Webhook Feature

### Enabling the Webhook Feature

PRISM Agent uses the following environment variables to manage webhook notifications:

| Name              | Description                                                              | Default |
|-------------------|--------------------------------------------------------------------------|--------|
| `WEBHOOK_URL`     | The webhook endpoint URL where the notifications will be sent            | null   |
| `WEBHOOK_API_KEY` | The optional API key (bearer token) to use as the `Authorization` header | null   |

### Securing the Webhook Endpoint

It is essential to secure the webhook endpoint to protect the integrity and confidentiality of the event data. Consider
the following best practices when securing your webhook endpoint:

- Use HTTPS to encrypt communication between PRISM Agent and the webhook endpoint.
- Implement authentication mechanisms (e.g., API keys, tokens) to verify the authenticity of incoming requests.
- Validate and sanitize incoming webhook requests to mitigate potential security risks.

The current supported authorization mechanism for PRISM Agent's webhook notifications is the bearer token. If
configured, the token will be included in the `Authorization` header of the HTTP request sent by the agent to the
webhook endpoint. You can configure this bearer token by setting the value of the `WEBHOOK_API_KEY` environment
variable.

## Event Format and Types

### Event Format

Webhook notifications from PRISM Agent are sent as JSON payloads in the HTTP requests.

The event format is consistent across all events. Each event follows a common structure, while the 'data' field
within the event payload contains information specific to the type of event. Here is an example of the JSON payload
format:

The event payload typically includes relevant details about the specific event that occurred within the agent. Below is
an example of the JSON payload format:

```json
{
  "id": "cb8d4e96-30f0-4892-863f-44d49d634211",
  "ts": "2023-07-06T12:01:19.769427Z",
  "eventType": "xxxx",
  "data": {
    // Event-specific data goes here 
  }
}
```

This event format ensures consistency and allows you to handle webhook notifications uniformly while easily extracting
the relevant data specific to each event type from the `data` field.

Here is an example of a webhook notification event related to a connection flow state change (invitation generated):

```json
{
  "id": "cb8d4e96-30f0-4892-863f-44d49d634211",
  "ts": "2023-07-06T12:01:19.769427Z",
  "eventType": "Connection",
  "data": {
    "connectionId": "c10787cf-99bb-47f4-99bb-1fdcca32b673",
    "label": "Connect with Alice",
    "role": "Inviter",
    "state": "InvitationGenerated",
    "invitation": {
      "id": "c10787cf-99bb-47f4-99bb-1fdcca32b673",
      "type": "https://didcomm.org/out-of-band/2.0/invitation",
      "from": "did:peer:2.Ez6LS...jIiXX0",
      "invitationUrl": "https://my.domain.com/path?_oob=eyJpZCI6...bXX19"
    },
    "createdAt": "2023-07-06T12:01:19.760126Z",
    "self": "c10787cf-99bb-47f4-99bb-1fdcca32b673",
    "kind": "Connection"
  }
}
```

### Common Event Types

PRISM Agent sends webhook notifications for events related to protocol state changes in
the [Connect](../connections/connection.md), [Issue](../credentials/issue.md),
and [Presentation](../credentials/present-proof.md) flows. These events allow you to track the progress and updates
within these flows in real-time. Some common event types that you can expect to receive through webhook notifications
include:

- Connection State Change: Notifies about state changes in the connection flow, such as `InvitationGenerated`,
  `ConnectionRequestSent`, `ConnectionResponseReceived`, etc. Please refer to the `state` field of
  the [connection resource](https://docs.atalaprism.io/agent-api/#tag/Connections-Management/operation/getConnection)
  for an exhaustive list of states.
- Credential State Change: Indicates changes in the credential issuance flow, such as `OfferSent`, `RequestReceived`,
  `CredentialSent`, etc. Please refer to the `protocolState` field of
  the [credential resource](https://docs.atalaprism.io/agent-api/#tag/Issue-Credentials-Protocol/operation/getCredentialRecord)
  for an exhaustive list of states.
- Presentation State Change: Notifies about changes in the presentation flow, such as `RequestReceived`,
  `PresentationGenerated`, `PresentationVerified`, etc. Please refer to the `status` field of
  the [presentation resource](https://docs.atalaprism.io/agent-api/#tag/Present-Proof/operation/getPresentation) for an
  exhaustive list of states.

## Processing Webhook Notifications

### Handling Incoming Webhook Requests

To handle incoming webhook notifications from PRISM Agent in your application, follow these general steps:

1. Receive the HTTP request at your specified webhook endpoint.
2. Parse the JSON payload of the request to extract the event details.
3. Process the event data according to your application's requirements.
4. Send a response back to acknowledge the successful receipt of the webhook notification. For a successful reception,
   the response status code should be `>= 200` and `< 300`. Any other response status code will lead to a new attempt
   from the PRISM Agent.

### Error Handling and Retry Mechanisms

When working with webhook notifications in PRISM Agent, it is important to consider error handling and retry mechanisms.
In case of failed webhook notifications or errors, PRISM Agent employs an automatic retry mechanism to ensure delivery.
The agent will attempt to send the webhook notification up to three times, with a five-second interval between each
attempt. Please note that the number of retries and the interval duration are currently not configurable in PRISM Agent.

By default, this retry mechanism provides a reasonable level of reliability for delivering webhook notifications,
allowing for temporary network issues or intermittent failures.

## Conclusion

Congratulations! You've learned about webhook notifications in PRISM Agent. By leveraging this feature, you can receive
real-time updates on events happening within the agent, enabling you to integrate PRISM Agent seamlessly into your
applications. Remember to secure your webhook endpoint and handle webhook notifications effectively to maximize the
benefits of this feature.

Start integrating webhook notifications into your PRISM Agent workflow and unlock the power of real-time event updates!

If you have any further questions or need assistance, don't hesitate to reach out to the PRISM Agent support team or
refer to the official documentation for more details.