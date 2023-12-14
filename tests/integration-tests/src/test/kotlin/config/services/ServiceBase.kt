package config.services

import com.sksamuel.hoplite.ConfigAlias
import org.testcontainers.containers.ComposeContainer

interface ServiceBase {

    val env: ComposeContainer

    @ConfigAlias("keep_running")
    val keepRunning: Boolean
    fun start() {
        env.start()
    }
    fun stop() {
        if (!keepRunning) {
            env.stop()
        }
    }
}
