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
    ANONCREDS_STUDENT_CLAIMS {
        override val claims: Map<String, Any> = linkedMapOf(
            "name" to "Bob",
            "age" to "21",
            "sex" to "M",
        )
    },
    ;

    abstract val claims: Map<String, Any>
}
