# BB containers
castorApi = container "Castor API Server" "Exposes a suite of decentralised identifier (DID) operations to create, manage and resolve standards based decentralised identifiers in a user-controlled manner" "Service" "Service BB" {
    didOpReqHandler = component "DID Operation Controller" "" "akka-http/zio-http"
    didOpComponent = component "DID Operation Component" "Provides a functionality related to DID operations: create, update, recover, deactivate"
    didResolveComponent = component "DID Resolution Component" "Provides a DID resolution capability, ensures DID is resovled in a consistent and deterministic way"
    !include shared/iris_client.dsl

    didOpReqHandler -> didOpComponent "Calls"
    didOpReqHandler -> didResolveComponent "Calls"
    didOpComponent -> irisClient "Schedules DID operations for publishing"
}
castorWorker = container "Castor DLT Worker" "Consumes events from a message queue and update Castor's state in a real-time manner. Also synchronizes Castor's state on startup or regular interval." "Service" "Service BB" {
    taskScheduler = component "Task Scheduler" "Manages the invocation of sync procedures. Triggers sync procedures on startup or regular interval for reconciliation."
    dltEventConsumer = component "DLT Event Consumer" "Consumes and parses DLT published events. Handles message queue transaction acknowledgement."
    opsProcessor = component "Operation Processor" "Provides real-time DID processing logic, parsing, validating, filtering when a DLT event has been observed. Updates Castor's state accordingly."
    publishedOpsSyncProc = component "Published Ops Sync Procedure" "Provides batch synchronization logic for published operations including windowing, pagination, throttling, rollback, conflict resolution, etc. Ensures that operations observed are in sync with data on chain"
    scheduledOpsSyncProc = component "Scheduled Ops Sync Procedure" "Provides batch synchronization logic for scheduled operation including cleaning dangling objects, reconciling corrupted state, etc."
    !include shared/iris_client.dsl

    dltEventConsumer -> opsProcessor "Invokes for each published event observed"
    dltEventConsumer -> publishedOpsSyncProc "Invokes for rollback event observed"
    dltEventConsumer -> scheduledOpsSyncProc "Invokes for rollback event observed"
    publishedOpsSyncProc -> irisClient "Fetches historical data"
    scheduledOpsSyncProc -> irisClient "Fetches DLT operation detail"
    taskScheduler -> publishedOpsSyncProc "Calls"
    taskScheduler -> scheduledOpsSyncProc "Calls"
}
castorDatabase = container "DID Operation Database" "Stores DID published operations / operations scheduled for publishing" "PostgreSQL" "Database"
castorCache = container "DID Resolution Cache" "Stores resolved DID documents." "Redis" "Database"

# relations within BB
castorApi.didOpComponent -> castorDatabase "Writes scheduled DID operations" "JDBC"
castorApi.didResolveComponent -> castorDatabase "Reads DID operations" "JDBC"
castorApi.didResolveComponent -> castorCache "Writes / Reads resolved DID Documents" "JDBC"

castorWorker.opsProcessor -> castorDatabase "Writes observed DID operations to" "JDBC"
castorWorker.opsProcessor -> castorCache "Invalidates cache" "JDBC"
castorWorker.scheduledOpsSyncProc -> castorDatabase "Updates operations" "JDBC"
castorWorker.publishedOpsSyncProc -> castorDatabase "Updates operations" "JDBC"
castorWorker.publishedOpsSyncProc -> castorCache "Invalidates cache" "JDBC"