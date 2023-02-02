package io.iohk.atala.test.container

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import zio.*
import zio.test.{TestAspect, TestAspectAtLeastR, TestAspectPoly}

object DBTestUtils {
  def runMigrationPgContainer(schema: String, paths: String*): RIO[PostgreSQLContainer, MigrateResult] =
    for {
      pg <- ZIO.service[PostgreSQLContainer]
      result <- runMigration(pg.jdbcUrl, pg.username, pg.password, schema, paths: _*)
    } yield result

  def runMigration(
      url: String,
      username: String,
      password: String,
      schema: String,
      locations: String*
  ): Task[MigrateResult] =
    ZIO.attemptBlocking {
      Flyway
        .configure()
        .dataSource(url, username, password)
        .schemas(schema)
        .locations(locations: _*)
        .load()
        .migrate()
    }
}
