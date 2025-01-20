package eu.europa.ec.eudi.openid4vci

import java.net.URI
import java.net.URL

/**
 * https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vci-kt/blob/e81802f3b90639b97e32a6fd1c06c20e5ff53f27/src/main/kotlin/eu/europa/ec/eudi/openid4vci/HttpsUrl.kt#L45
 *
 * This overrides the implementation in EUDI to relax the HTTPS requirement making it easier for testing purpose
 */
@JvmInline
value class HttpsUrl private constructor(val value: URL) {

    override fun toString(): String = value.toString()

    companion object {

        /**
         * Parses the provided [value] as a [URI] and tries creates a new [HttpsUrl].
         */
        operator fun invoke(value: String): Result<HttpsUrl> = runCatching {
            val uri = URI.create(value)
            // require(uri.scheme.contentEquals("https", true)) { "URL must use https protocol" }
            HttpsUrl(uri.toURL())
        }
    }
}
