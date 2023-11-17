package config

data class Config(
    val global: GlobalConf,
    val admin: AgentConf,
    val issuer: AgentConf,
    val holder: AgentConf,
    val verifier: AgentConf,
    val agents: List<AgentInitConf>,
    val services: ServicesConf
)
