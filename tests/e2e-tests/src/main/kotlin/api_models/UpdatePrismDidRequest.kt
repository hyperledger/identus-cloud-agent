package api_models

data class UpdatePrismDidRequest(
    val actions: List<UpdatePrismDidAction>,
)

data class UpdatePrismDidAction(
    val actionType: String? = null,
    val addKey: PublicKey? = null,
    val removeKey: PublicKey? = null,
    val addService: Service? = null,
    val removeService: Service? = null,
    val updateService: Service? = null,
)
