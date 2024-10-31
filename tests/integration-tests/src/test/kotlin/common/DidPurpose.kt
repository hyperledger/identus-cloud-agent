package common

import org.hyperledger.identus.client.models.*

enum class DidPurpose {
    CUSTOM {
        override val publicKeys = mutableListOf<ManagedDIDKeyTemplate>()
        override val services = mutableListOf<Service>()
    },
    SD_JWT {
        override val publicKeys = mutableListOf(
            ManagedDIDKeyTemplate("auth-1", Purpose.AUTHENTICATION, Curve.ED25519),
            ManagedDIDKeyTemplate("assertion-1", Purpose.ASSERTION_METHOD, Curve.ED25519),
        )
        override val services = mutableListOf<Service>()
    },
    JWT {
        override val publicKeys = mutableListOf(
            ManagedDIDKeyTemplate("auth-1", Purpose.AUTHENTICATION, Curve.SECP256K1),
            ManagedDIDKeyTemplate("auth-2", Purpose.AUTHENTICATION, Curve.ED25519),
            ManagedDIDKeyTemplate("assertion-1", Purpose.ASSERTION_METHOD, Curve.SECP256K1),
            ManagedDIDKeyTemplate("assertion-2", Purpose.ASSERTION_METHOD, Curve.ED25519),
        )
        override val services = mutableListOf<Service>()
    },
    OIDC_JWT {
        override val publicKeys = mutableListOf(
            ManagedDIDKeyTemplate("auth-1", Purpose.AUTHENTICATION, Curve.SECP256K1),
            ManagedDIDKeyTemplate("auth-2", Purpose.AUTHENTICATION, Curve.ED25519),
            ManagedDIDKeyTemplate("assertion-1", Purpose.ASSERTION_METHOD, Curve.SECP256K1),
        )
        override val services = mutableListOf<Service>()
    },
    ANONCRED {
        override val publicKeys = mutableListOf<ManagedDIDKeyTemplate>()
        override val services = mutableListOf<Service>()
    }, ;

    abstract val publicKeys: MutableList<ManagedDIDKeyTemplate>
    abstract val services: MutableList<Service>
}
