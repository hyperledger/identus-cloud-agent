package io.iohk.atala.test.container

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import zio.*

trait PostgresTestContainerSupport {

  protected val pgContainerLayer: ULayer[PostgreSQLContainer] = PostgresLayer.postgresLayer()

  protected val transactorLayer: TaskLayer[Transactor[Task]] =
    pgContainerLayer >>> PostgresLayer.hikariConfigLayer >>> PostgresLayer.transactor

}
