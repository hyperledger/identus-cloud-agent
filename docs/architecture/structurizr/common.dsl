workspace {

    model {
        
        didSubject = person "DID Subject" "The entity identified by a DID and described by a DID document"
        didController = person "DID Controller" "An entity that has the capability to make changes to a DID document"
        vcIssuer = person "VC Issuer" "A verified entity that can Issue verifiable credentials"
        vcHolder = person "VC Holder" "A user with a wallet that wants to keep their credentials decentralised and private"
        vcVerifier = person "VC Verifier" "Wants to identify if a credential or part of a credential is valid"
        
        enterprise "IOG" {
            atalaPrism = softwareSystem "Atala PRISM" "Exposes a suite of operations to create, manage and resolve standards based DIDs in a user-controlled manner, and a suite of operations to issue, manage and verify standards based VCs in a privacy preserving manner" {
                #wallet = container "Wallet App"
                #edgeAgent = container "Edge Agent"
                #cloudAgent = container "Cloud Agent"

                apiGateway = container "API Gateway" "Acts as the the entry point to API exposed by the backend microservices. Main functions: API backend documentation, TLS endpoint, reverse proxy, authentication & authorization, traffic monitoring, rate limiting, etc" "Middleware"
                
                castor = container "Castor" "Exposes a suite of decentralised identifier (DID) operations to create, manage and resolve standards based decentralised identifiers in a user-controlled manner" "Service BB"
                pollux = container "Pollux" "Exposes a suite of credential operations to issue, manage and verify standards based verifiable credentials in a privacy preserving manner" "Service BB"
                mercury = container "Mercury" "Exposes secure, standards based communications protocols to establish and manage trusted, peer-to-peer connections and interactions between DIDs in a transport agnostic and interoperable manner" "Service BB"
                pluto = container "Pluto" "Exposes storage operations to securely store, manage, and recover verifiable data linked to DIDs in a portable, storage agnostic manner" "Service BB"
                apollo = container "Apollo" "A suite of cryptographic primitives to ensure properties of integrity, authenticity and confidentiality of any data we store and process in a provably secure manner (* provides this to all components -> embedded)" "Library BB"
                athena = container "Athena" "A self-improving machine learning building block to increase the intelligence of data-driven predictive processes in a privacy preserving manner" "???"
                
                dlt = group "DTL Proxy" {
                    dltProxy = container "DLT Proxy" "Exposes a suite of operations allowing interactions with the underlying distributed ledger technology (e.g. Blockchain)" "Service"
                    msgQueue = container "Message Queue Middleware" "Notifies subscribers of changes happening on the underlying DLT" "Kafka?" "Existing Container"
                    cardanoWalletBackend = container "Cardano Wallet Backend" "Provides an HTTP API (and a CLI) for working with the wallet. It can be used as a component of a frontend such as Daedalus, which provides a friendly user interface for wallets" "Service" "Existing Container"
                    cardanoDbSync = container "Cardano DB Sync" "Provides a convenient way to find and query historical information from the Cardano blockchain through the use of a Structured Query Language (SQL) relational database" "Service" "Existing Container"
                    cardanoDbSyncDatabase = container "Cardano DB Sync Database" "" "PostgreSQL" "Existing Container"
                    cardanoNode = container "Cardano Node" "Core component that underpins the Cardano network. Aggregates other components: consensus, ledger and networking, with configuration, CLI, logging and monitoring" "Service" "Existing Container"
                }
            }
            
            cardanoDLT = softwareSystem "Cardano Blockchain" "" "Existing System"
        }

        # relations between people and software systems
        didSubject -> atalaPRISM "Uses"
        didController -> atalaPRISM "Makes changes to a DID document using"
        vcIssuer -> atalaPRISM "Issues verifiable credentials to holder using"
        vcHolder -> atalaPRISM "Keeps credentials, shares credentials or partial info on them with others using"
        vcVerifier -> atalaPRISM "Verifies credentials or partial credentials"

        didSubject -> apiGateway "Makes API calls to" "REST/HTTPS"
        didController -> apiGateway "Makes API calls to" "REST/HTTPS"
        vcIssuer -> apiGateway "Makes API calls to" "REST/HTTPS"
        vcHolder -> apiGateway "Makes API calls to" "REST/HTTPS"
        vcVerifier -> apiGateway "Makes API calls to" "REST/HTTPS"

        # relations to/from containers
        apiGateway -> castor "Routes requests to" "REST/HTTPS"
        apiGateway -> pollux "Routes requests to" "REST/HTTPS"
        apiGateway -> mercury "Routes requests to" "REST/HTTPS"
        apiGateway -> pluto "Routes requests to" "REST/HTTPS"

        castor -> dltProxy "Makes API calls to" "gRPC/HTTP2"
        msgQueue -> castor "Notifies DLT changes to"
        pollux -> dltProxy "Makes API calls to" "gRPC/HTTP2"
        msgQueue -> pollux "Notifies DLT changes to"

        dltProxy -> cardanoDbSyncDatabase "Reads from" "JDBC"
        dltProxy -> cardanoWalletBackend "Makes API calls to" "REST/HTTPS"
        cardanoDbSync -> cardanoDbSyncDatabase "Writes to" "JDBC"
        cardanoDbSync -> cardanoNode "Communicates with" "IPC socket"
        cardanoWalletBackend -> cardanoNode "Communicates with" "IPC socket"
        cardanoNode -> cardanoDLT "Interacts with" "TCP"

        # relations to/from components
    }

    views {
        systemContext atalaPRISM "SystemContext" {
            include *
            autoLayout
        }

        container atalaPRISM "Containers" {
            include *
            autoLayout
        }

        theme default

        styles { 
            element "Existing System" {
                background #999999
                color #ffffff
            }
            element "Existing Container" {
                background #999999
                color #ffffff
            }      
        }
    }

}
