package models

data class SdJwtClaim(
    val salt: String,
    val key: String,
    val value: String,
)
