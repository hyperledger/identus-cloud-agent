package config

import config.services.Agent
import config.services.Service

data class Config(
    val roles: List<Role>,
    val agents: List<Agent>?,
    val services: Service?,
)
