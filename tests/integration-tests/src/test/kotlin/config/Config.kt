package config

data class Config(
    val global: GlobalConf,
    val issuer: AgentConf,
    val holder: AgentConf,
    val verifier: AgentConf,
    val admin: AgentConf
)
