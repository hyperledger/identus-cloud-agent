package api_models

import com.fasterxml.jackson.annotation.JsonValue

data class PublicKey(
    val id: String,
    val purpose: Purpose
)

enum class Purpose(@JsonValue val value: String) {
    AUTHENTICATION("authentication"),
    ASSERTION_METHOD("assertionMethod"),
    KEY_AGREEMENT("keyAgreement"),
    CAPABILITY_INVOCATION("capabilityInvocation"),
    CAPABILITY_DELEGATION("capabilityDelegation")
}
