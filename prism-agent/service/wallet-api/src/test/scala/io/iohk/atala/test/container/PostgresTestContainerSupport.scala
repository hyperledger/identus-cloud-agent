package io.iohk.atala.test.container

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import zio.*

trait PostgresTestContainerSupport {

  protected val pgContainerLayer: ULayer[PostgreSQLContainer] = PostgresTestContainer.postgresLayer()

  protected val transactorLayer: TaskLayer[Transactor[Task]] =
    pgContainerLayer >>> PostgresTestContainer.hikariConfigLayer >>> PostgresTestContainer.transactor

}
