package common

enum class CredentialType {
    JWT_VCDM_1_1 {
        override val format: String = "JWT"
    },
    JWT_VCDM_2_0 {
        override val format: String = "JWT"
    },
    ANONCREDS_V1 {
        override val format: String = "AnonCreds"
    },
    SD_JWT_VCDM_1_1 {
        override val format: String = "SDJWT"
    },
    ;

    abstract val format: String
}
