package common

import org.hyperledger.identus.client.models.*

enum class DidPurpose {
    EMPTY {
        override val publicKeys = emptyList<ManagedDIDKeyTemplate>()
        override val services = emptyList<Service>()
    },
    JWT {
        override val publicKeys = listOf(
            ManagedDIDKeyTemplate("auth-1", Purpose.AUTHENTICATION, Curve.SECP256K1),
            ManagedDIDKeyTemplate("auth-2", Purpose.AUTHENTICATION, Curve.ED25519),
            ManagedDIDKeyTemplate("assertion-1", Purpose.ASSERTION_METHOD, Curve.SECP256K1),
        )
        override val services = emptyList<Service>()
    },
    ANONCRED {
        override val publicKeys = emptyList<ManagedDIDKeyTemplate>()
        override val services = emptyList<Service>()
    };

    abstract val publicKeys: List<ManagedDIDKeyTemplate>
    abstract val services: List<Service>
}
