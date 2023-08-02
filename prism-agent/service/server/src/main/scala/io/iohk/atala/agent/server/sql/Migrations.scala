package io.iohk.atala.agent.server.sql

import org.flywaydb.core.Flyway
import zio.*
final case class Migrations(config: DbConfig) {

  val migrationScriptsLocation: String = "sql/agent"

  def migrate: Task[Unit] =
    for {
      _ <- ZIO.logInfo("Applying database migrations")
      _ <- ZIO.attempt {
        Flyway
          .configure()
          .dataSource(
            config.jdbcUrl,
            config.username,
            config.password
          )
          .locations(migrationScriptsLocation)
          .load()
          .migrate()
      }
    } yield ()

}

object Migrations {
  val layer: URLayer[DbConfig, Migrations] =
    ZLayer.fromFunction(Migrations.apply _)
}
