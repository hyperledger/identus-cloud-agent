package common

import org.hyperledger.identus.client.models.*

enum class DidType {
    CUSTOM {
        override val documentTemplate get() = DidDocumentTemplate(
            publicKeys = mutableListOf(),
            services = mutableListOf(),
        )
    },
    SD_JWT {
        override val documentTemplate get() = DidDocumentTemplate(
            publicKeys = mutableListOf(
                ManagedDIDKeyTemplate("auth-1", Purpose.AUTHENTICATION, Curve.ED25519),
                ManagedDIDKeyTemplate("assertion-1", Purpose.ASSERTION_METHOD, Curve.ED25519),
            ),
            services = mutableListOf(),
        )
    },
    JWT {
        override val documentTemplate get() = DidDocumentTemplate(
            publicKeys = mutableListOf(
                ManagedDIDKeyTemplate("auth-1", Purpose.AUTHENTICATION, Curve.SECP256K1),
                ManagedDIDKeyTemplate("auth-2", Purpose.AUTHENTICATION, Curve.ED25519),
                ManagedDIDKeyTemplate("assertion-1", Purpose.ASSERTION_METHOD, Curve.SECP256K1),
                ManagedDIDKeyTemplate("assertion-2", Purpose.ASSERTION_METHOD, Curve.ED25519),
            ),
            services = mutableListOf(),
        )
    },
    OIDC_JWT {
        override val documentTemplate get() = DidDocumentTemplate(
            publicKeys = mutableListOf(
                ManagedDIDKeyTemplate("auth-1", Purpose.AUTHENTICATION, Curve.SECP256K1),
                ManagedDIDKeyTemplate("auth-2", Purpose.AUTHENTICATION, Curve.ED25519),
                ManagedDIDKeyTemplate("assertion-1", Purpose.ASSERTION_METHOD, Curve.SECP256K1),
            ),
            services = mutableListOf(),
        )
    },
    ANONCRED {
        override val documentTemplate get() = DidDocumentTemplate(
            publicKeys = mutableListOf(),
            services = mutableListOf(),
        )
    }, ;

    abstract val documentTemplate: DidDocumentTemplate
}
