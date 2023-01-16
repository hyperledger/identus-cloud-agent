# BB containers
iris = container "Iris" "Exposes a suite of operations allowing interactions with the underlying distributed ledger technology (e.g. Blockchain)" "Service"
msgQueue = container "Message Queue Middleware" "Notifies subscribers of changes happening on the underlying DLT" "Kafka?" "Existing Container, Message Queue"
cardanoWalletBackend = container "Cardano Wallet Backend" "Provides an HTTP API (and a CLI) for working with the wallet. It can be used as a component of a frontend such as Daedalus, which provides a friendly user interface for wallets" "Service" "Existing Container"
cardanoDbSync = container "Cardano DB Sync" "Provides a convenient way to find and query historical information from the Cardano blockchain through the use of a Structured Query Language (SQL) relational database" "Service" "Existing Container"
cardanoDbSyncDatabase = container "Cardano DB Sync Database" "" "PostgreSQL" "Existing Container, Database"
cardanoNode = container "Cardano Node" "Core component that underpins the Cardano network. Aggregates other components: consensus, ledger and networking, with configuration, CLI, logging and monitoring" "Service" "Existing Container"

# relations within BB
iris -> cardanoDbSyncDatabase "Reads from" "JDBC"
iris -> cardanoWalletBackend "Makes API calls to" "REST/HTTPS"
cardanoDbSync -> cardanoDbSyncDatabase "Writes to" "JDBC"
cardanoDbSync -> cardanoNode "Communicates with" "IPC socket"
cardanoWalletBackend -> cardanoNode "Communicates with" "IPC socket"
