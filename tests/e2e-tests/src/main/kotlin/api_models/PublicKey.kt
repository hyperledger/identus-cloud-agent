package api_models

import com.fasterxml.jackson.annotation.JsonValue

enum class Purpose(@JsonValue val value: String) {
    AUTHENTICATION("authentication"),
    ASSERTION_METHOD("assertionMethod"),
    KEY_AGREEMENT("keyAgreement"),
    CAPABILITY_INVOCATION("capabilityInvocation"),
    CAPABILITY_DELEGATION("capabilityDelegation")
}

data class PublicKey(
    val id: String,
    val purpose: Purpose
)
