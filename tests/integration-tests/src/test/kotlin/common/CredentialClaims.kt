package common

enum class CredentialClaims {
    STUDENT_CLAIMS {
        override val claims: Map<String, Any> = linkedMapOf(
            "name" to "Name",
            "age" to 18,
        )
    },
    ID_CLAIMS {
        override val claims: Map<String, Any> = linkedMapOf(
            "firstName" to "John",
            "lastName" to "Doe",
        )
    },
    ;

    abstract val claims: Map<String, Any>
}
