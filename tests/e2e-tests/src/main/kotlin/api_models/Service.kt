package api_models

data class Service(
    var id: String = "",
    var serviceEndpoint: List<String> = listOf(""),
    var type: String = "",
)
