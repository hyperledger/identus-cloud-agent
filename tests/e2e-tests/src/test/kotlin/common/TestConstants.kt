package common

import api_models.PublicKey
import api_models.Purpose
import api_models.Service
import java.time.Duration
import java.util.UUID

object TestConstants {
    val CREDENTIAL_SCHEMAS = CredentialSchemas
    val RANDOM_CONSTAND_UUID = UUID.randomUUID().toString()
    val DID_UPDATE_PUBLISH_MAX_WAIT_5_MIN = Duration.ofSeconds(600L)
    var PRISM_DID_AUTH_KEY = PublicKey("auth_key1", Purpose.AUTHENTICATION)
    val PRISM_DID_ASSERTION_KEY = PublicKey("assertion_key1", Purpose.ASSERTION_METHOD)
    val PRISM_DID_UPDATE_NEW_AUTH_KEY = PublicKey("new_auth_key", Purpose.AUTHENTICATION)
    val PRISM_DID_SERVICE = Service(
        "https://foo.bar.com",
        listOf("https://foo.bar.com/"),
        "LinkedDomains",
    )
    val PRISM_DID_UPDATE_NEW_SERVICE_URL = "https://bar.foo.com/"
    val PRISM_DID_UPDATE_NEW_SERVICE = Service(
        "https://new.service.com",
        listOf("https://new.service.com/"),
        "LinkedDomains",
    )
    var PRISM_DID_FOR_UPDATES: String? = null
    var PRISM_DID_FOR_DEACTIVATION: String? = null
}
