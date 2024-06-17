package config.services

import com.sksamuel.hoplite.ConfigAlias
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.lifecycle.Startable

interface ServiceBase : Startable {

    val container: ComposeContainer

    @ConfigAlias("keep_running")
    val keepRunning: Boolean

    override fun start() {
        container.start()
        postStart()
    }

    fun postStart() {
    }

    override fun stop() {
        if (!keepRunning) {
            container.stop()
        }
    }
}
