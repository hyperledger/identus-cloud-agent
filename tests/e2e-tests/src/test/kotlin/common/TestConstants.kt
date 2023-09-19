package common

import api_models.PublicKey
import api_models.Purpose
import api_models.Service
import java.time.Duration
import java.util.*

object TestConstants {
    val VERIFICATION_POLICIES = VerificationPolicies
    val CREDENTIAL_SCHEMAS = CredentialSchemas
    val RANDOM_CONSTAND_UUID = UUID.randomUUID().toString()
    val DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN = Duration.ofSeconds(60L)
    val PRISM_DID_AUTH_KEY = PublicKey("auth-1", Purpose.AUTHENTICATION)
    val PRISM_DID_ASSERTION_KEY = PublicKey("assertion-1", Purpose.ASSERTION_METHOD)
    val PRISM_DID_UPDATE_NEW_AUTH_KEY = PublicKey("auth-2", Purpose.AUTHENTICATION)
    val PRISM_DID_SERVICE = Service(
        "https://foo.bar.com",
        listOf("https://foo.bar.com/"),
        "LinkedDomains",
    )
    val PRISM_DID_SERVICE_FOR_UPDATE = Service(
        "https://update.com",
        listOf("https://update.com/"),
        "LinkedDomains",
    )
    val PRISM_DID_SERVICE_TO_REMOVE = Service(
        "https://remove.com",
        listOf("https://remove.com/"),
        "LinkedDomains",
    )
    val PRISM_DID_UPDATE_NEW_SERVICE_URL = "https://bar.foo.com/"
    val PRISM_DID_UPDATE_NEW_SERVICE = Service(
        "https://new.service.com",
        listOf("https://new.service.com/"),
        "LinkedDomains",
    )
    val EVENT_TYPE_CONNECTION_UPDATED = "ConnectionUpdated"
    val EVENT_TYPE_ISSUE_CREDENTIAL_RECORD_UPDATED = "IssueCredentialRecordUpdated"
    val EVENT_TYPE_PRESENTATION_UPDATED = "PresentationUpdated"
    val EVENT_TYPE_DID_STATUS_UPDATED = "DIDStatusUpdated"
    val WRONG_SEED = "wrong seed"
}
