workspace {

    !identifiers hierarchical

    model {

        didSubject = person "DID Subject" "The entity identified by a DID and described by a DID document"
        didController = person "DID Controller" "An entity that has the capability to make changes to a DID document"
        vcIssuer = person "VC Issuer" "A verified entity that can Issue verifiable credentials"
        vcHolder = person "VC Holder" "A user with a wallet that wants to keep their credentials decentralised and private"
        vcVerifier = person "VC Verifier" "Wants to identify if a credential or part of a credential is valid"

        enterprise "IOG" {
            atalaPrism = softwareSystem "Atala PRISM" "Exposes a suite of operations to create, manage and resolve standards based DIDs in a user-controlled manner, and a suite of operations to issue, manage and verify standards based VCs in a privacy preserving manner" {
                mobileApp = container "Mobile App" "" "" "Mobile App" {
                    appCode = component "Mobile App Logic"

                    # Reference: https://livebook.manning.com/book/self-sovereign-identity/chapter-9/36
                    # A digital agent is to a digital wallet what an operating system is to a computer or smart-phone.
                    # It is the software that enables a person to take actions, perform communications, store information, and track usage of the digital wallet.
                    edgeAgent = component "Edge Agent" "A software that enables a person to take actions, perform communications, store information, and track usage of the digital wallet"
                    wallet = component "Wallet" "A software (and optionally hardware) that enables the wallet’s controller to generate, store, manage, and protect cryptographic keys, secrets, and other sensitive private data"
                    bbSDK = component "Building Block SDK" "Client side logic for BBs"
                    bbClient = component "Building Block HTTP Client" "OpenAPI generated stubs for all BBs"

                    # relations within container
                    appCode -> edgeAgent "Operates"
                    edgeAgent -> wallet "Uses"
                    edgeAgent -> bbSDK "Uses"
                    bbSDK -> bbClient "Uses"
                }

                apiGateway = container "API Gateway" "Acts as the the entry point to API exposed by the backend microservices. Main functions: API backend documentation, TLS endpoint, reverse proxy, authentication & authorization, traffic monitoring, rate limiting, etc" "Middleware"

                pollux = container "Pollux" "Exposes a suite of credential operations to issue, manage and verify standards based verifiable credentials in a privacy preserving manner" "Service BB"
                mercury = container "Mercury" "Exposes secure, standards based communications protocols to establish and manage trusted, peer-to-peer connections and interactions between DIDs in a transport agnostic and interoperable manner" "Service BB"
                pluto = container "Pluto" "Exposes storage operations to securely store, manage, and recover verifiable data linked to DIDs in a portable, storage agnostic manner" "Service BB"
                apollo = container "Apollo" "A suite of cryptographic primitives to ensure properties of integrity, authenticity and confidentiality of any data we store and process in a provably secure manner (* provides this to all components -> embedded)" "Library BB"
                athena = container "Athena" "A self-improving machine learning building block to increase the intelligence of data-driven predictive processes in a privacy preserving manner" "???"

                castorGroup = group "Castor" {
                    !include castor_containers.dsl
                }

                dltGroup = group "DTL Proxy" {
                    !include iris_containers.dsl
                }

                # relations to/from containers within Prism
                apiGateway -> castorApi "Routes requests to" "REST/HTTP"
                apiGateway -> pollux "Routes requests to" "REST/HTTP"
                apiGateway -> mercury "Routes requests to" "REST/HTTP"
                apiGateway -> pluto "Routes requests to" "REST/HTTP"

                castorApi -> iris "Makes API calls to" "gRPC/HTTP2"
                pollux -> iris "Makes API calls to" "gRPC/HTTP2"
                msgQueue -> pollux "Notifies DLT changes to"

                # relations to/from components within Prism
                mobileApp.edgeAgent -> apiGateway "Communicates with Mercury Cloud Agent" "DIDComm"
                mobileApp.bbClient -> apiGateway "Makes API calls to BB" "REST/HTTPS"
                msgQueue -> castorWorker.dltEventConsumer "Notifies DLT changes to"
            }

            cardanoDLT = softwareSystem "Cardano Blockchain" "" "Existing System"

            # relations to/from software system within IOG
            atalaPrism.cardanoNode -> cardanoDLT "Interacts with" "TCP"
        }

        # relations between people and software systems
        didSubject -> atalaPrism "Uses"
        didController -> atalaPrism "Makes changes to a DID document using"
        vcIssuer -> atalaPrism "Issues verifiable credentials to holder using"
        vcHolder -> atalaPrism "Keeps credentials, shares credentials or partial info on them with others using"
        vcVerifier -> atalaPrism "Verifies credentials or partial credentials"

        didSubject -> atalaPrism.mobileApp "Uses"
        didController -> atalaPrism.mobileApp "Uses"
        vcIssuer -> atalaPrism.mobileApp "Uses"
        vcHolder -> atalaPrism.mobileApp "Uses"
        vcVerifier -> atalaPrism.mobileApp "Uses"
    }

    views {
        systemContext atalaPrism "SystemContext" {
            include *
            autoLayout
        }

        container atalaPrism "BBContainers" "Building Block Containers" {
            include *
            exclude element.tag==Database
            exclude "element.tag==Message Queue"
            exclude "element.tag==Existing Container"
            exclude "element.tag==Existing System"
            autoLayout
        }

        container atalaPrism "Castor" "Castor Container" {
            include ->atalaPrism.castorGroup atalaPrism.castorGroup->
            autoLayout
        }

        container atalaPrism "Iris" "Iris Container" {
            include ->atalaPrism.dltGroup atalaPrism.dltGroup->
            autoLayout
        }

        component atalaPrism.mobileApp "Mob" "Mobile App Components" {
            include *
            autoLayout
        }

        component atalaPrism.castorApi "CastorAPI" "Castor API Components" {
            include *
            autoLayout
        }

        component atalaPrism.castorWorker "CastorWorker" "Castor Worker Component" {
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
            element "Database" {
                shape Cylinder
            }
            element "Message Queue" {
                shape Pipe
            }
            element "Mobile App" {
                shape MobileDeviceLandscape
            }
        }
    }

}