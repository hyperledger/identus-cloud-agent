package io.iohk.atala.pollux.sql.repository

import org.flywaydb.core.Flyway
import zio.*

import javax.sql.DataSource

final case class Migrations(config: DbConfig) {

  val migrationScriptsLocation: String = "sql/pollux"

  def migrate: Task[Unit] = 
    ZIO.logInfo("Applying database migrations")
    for {
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
