package config

import config.services.Agent
import config.services.Service

data class Config(
<<<<<<< HEAD
    val roles: List<Role>,
    val agents: List<Agent>?,
    val services: Service?,
=======
    val global: GlobalConf,
    val admin: AgentConf,
    val issuer: AgentConf,
    val holder: AgentConf,
    val verifier: AgentConf,
    val agents: List<AgentInitConf>,
    val services: ServicesConf
>>>>>>> 7fd03ce4 (test: configurable integration tests support (#772))
)
