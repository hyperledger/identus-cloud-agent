package io.iohk.atala.test.container

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import zio.*
import io.iohk.atala.shared.db.ContextAwareTask

trait PostgresTestContainerSupport {

  protected val pgContainerLayer: TaskLayer[PostgreSQLContainer] = PostgresLayer.postgresLayer()

  protected val transactorLayer: TaskLayer[Transactor[ContextAwareTask]] =
    pgContainerLayer >>> PostgresLayer.hikariConfigLayer >>> PostgresLayer.transactor

}
