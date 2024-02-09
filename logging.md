# Logging

We want traceability from HTTP calls.
- Each HTTP call needs to generate a call ID (preferable on the proxy). This ID must be should be pass to the scope of the ZIO application. So every log will mention this ID. This ID must also be returned to the user as a HTTP header.
- In case of errors and problem reports those IDs can be use by the user to get support.

Level 3 support can be provided to user during the logging retention policies. We user need to provide the value in the header `X-Request-Id` to get this support.

## Annotates

We have special annotations in the log so there is traceability between the different logs. 
Here is the list of annotations and their meaning that we currently have:

- `request-id` - Is the HTTP header `X-Request-Id` from the caller.
  - If this header is missing, it is create by the APISIX https://apisix.apache.org/docs/apisix/plugins/request-id/. See the configuration how to enable in the file [apisixroute.yaml](./infrastructure/charts/agent/templates/apisixroute.yaml)

## Code

To have a concise code we have created a Middleware that modifies the Annotations in the Scope before each execution of that endpoint, to include the trace ID of the request.
See code in file `LogUtils.scala`.

## Logging Backend

We use the Simple Logging Facade for Java (SLF4J) API to call the logging backend is determined at runtime.

### Logging Pattern

`%d{yyyy-MM-dd_HH:mm:ss.SSS} %highlight(%-5level) %cyan(%logger{5}@L%line:[%-4.30thread]) {%mdc} - %msg%xException%n`

- `%d{yyyy-MM-dd_HH:mm:ss.SSS}` is the date/timestamp of the log in the human-readable way
- `%highlight(%-5level)` the log level
- `%cyan(%logger{5}@L%line:[%-4.30thread])`
  - `%cyan` - Is just modify the color to make easy to read
  - `%logger{5}` - class name that originated the log
  - `@L%line` - line of the code that originated the log
  - `%-4.30thread` - the id of the thread. The ZIO fiber name
- `%mdc` - the mapped diagnostic context [MDC](https://logback.qos.ch/manual/layouts.html#mdc)
- `%msg` - the log message
- `%xException` - exception info

## APIs with logging

- Credential Definition Registry
  - GET - https://docs.atalaprism.io/credential-definition-registry/definitions
  - POST - https://docs.atalaprism.io/credential-definition-registry/definitions
  - GET - https://docs.atalaprism.io/credential-definition-registry/definitions/{guid}
  - GET - https://docs.atalaprism.io/credential-definition-registry/definitions/{guid}/definition
- Schema Registry
  - GET - https://docs.atalaprism.io/schema-registry/schemas
  - PPOST - https://docs.atalaprism.io/schema-registry/schemas
  - PUT - https://docs.atalaprism.io/schema-registry/{author}/{id}
  - GET - https://docs.atalaprism.io/schema-registry/schemas/{guid}
  - ??? TODO ??? GET - https://docs.atalaprism.io/schema-registry/test (this endpoint is in the list but I can't find the code for it)
- Verification
  - GET - https://docs.atalaprism.io/verification/policies
  - POST - https://docs.atalaprism.io/verification/policies
  - GET - https://docs.atalaprism.io/verification/policies/{id}
  - PUT - https://docs.atalaprism.io/verification/policies/{id}
  - DEL - https://docs.atalaprism.io/verification/policies/{id}
- Connections Management
  - GET - https://docs.atalaprism.io/connections
  - POST - https://docs.atalaprism.io/connections
  - GET - https://docs.atalaprism.io/connections/{connectionId}
  - POST -  https://docs.atalaprism.io/connection-invitations
- DID
  - https://docs.atalaprism.io/dids/{didRef}
- DID Registrar
  - GET - https://docs.atalaprism.io/did-registrar/dids
  - POST - https://docs.atalaprism.io/did-registrar/dids
  - GET - https://docs.atalaprism.io/did-registrar/dids/{didRef}
  - POST - https://docs.atalaprism.io/did-registrar/dids/{didRef}/publications
  - POST - https://docs.atalaprism.io/did-registrar/dids/{didRef}/updates
  - POST - https://docs.atalaprism.io/did-registrar/dids/{didRef}/deactivations
- Issue Credentials Protocol
  - POST - https://docs.atalaprism.io/issue-credentials/credential-offers
  - GET - https://docs.atalaprism.io/issue-credentials/records
  - GET - https://docs.atalaprism.io/issue-credentials/records/{recordId}
  - POST - https://docs.atalaprism.io/issue-credentials/records/{recordId}/accept-offer
  - POST - https://docs.atalaprism.io/issue-credentials/records/{recordId}/issue-credential
- Present Proof
  - GET - https://docs.atalaprism.io/present-proof/presentations
  - POST - https://docs.atalaprism.io/present-proof/presentations
  - GET - https://docs.atalaprism.io/present-proof/presentations/{presentationId}
  - PATH - https://docs.atalaprism.io/present-proof/presentations/{presentationId}
- System
  - GET - https://docs.atalaprism.io/_system/health
  - GET - https://docs.atalaprism.io/_system/metrics
- Identity and Access Management
  - GET - https://docs.atalaprism.io/iam/entities
  - POST - https://docs.atalaprism.io/iam/entities
  - PUT - https://docs.atalaprism.io/iam/entities/{id}/name
  - PUT - https://docs.atalaprism.io/iam/entities/{id}/walletId
  - GET - https://docs.atalaprism.io/iam/entities/{id}
  - DEL - https://docs.atalaprism.io/iam/entities/{id}
  - POST - https://docs.atalaprism.io/iam/apikey-authentication
  - DEL - https://docs.atalaprism.io/iam/apikey-authentication
- Wallet Management
  - GET - https://docs.atalaprism.io/wallets
  - POST - https://docs.atalaprism.io/wallets
  - GET - https://docs.atalaprism.io/wallets/{walletId}
  - POST - https://docs.atalaprism.io/wallets/{walletId}/uma-permissions
  - DEL - https://docs.atalaprism.io/wallets/{walletId}/uma-permissions
- Events
  - GET - https://docs.atalaprism.io/events/webhooks
  - POST - https://docs.atalaprism.io/events/webhooks
  - DEL - https://docs.atalaprism.io/events/webhooks/{id}
