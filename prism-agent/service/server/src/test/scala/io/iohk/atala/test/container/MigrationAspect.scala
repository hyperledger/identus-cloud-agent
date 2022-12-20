package io.iohk.atala.test.container

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.flywaydb.core.Flyway
import zio.ZIO
import zio.test.TestAspect
import zio.test.TestAspect.before

object MigrationAspects {
  def migrate(schema: String, paths: String*) = {
    val migration = for {
      pg <- ZIO.service[PostgreSQLContainer]
      _ <- runMigration(pg.jdbcUrl, pg.username, pg.password, schema, paths: _*)
    } yield ()

    before(migration.orDie)
  }

  def runMigration(
      url: String,
      username: String,
      password: String,
      schema: String,
      locations: String*
  ) =
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
