package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import io.iohk.atala.shared.db.DbConfig
import org.flywaydb.core.Flyway
import zio.*
import zio.interop.catz.*

final case class Migrations(config: DbConfig) {

  val migrationScriptsLocation: String = "sql/pollux"

  def migrate: Task[Unit] =
    for {
      _ <- ZIO.logInfo("Applying database migrations")
      _ <- ZIO.attempt {
        Flyway
          .configure()
          .loadDefaultConfigurationFiles()
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

  def initDbPrivileges(appUser: String): RIO[Transactor[Task], Unit] = {
    val cxnIO = for {
      _ <- doobie.free.connection.createStatement.map { stm =>
        stm.execute(s"""
          | ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "$appUser"
          """.stripMargin)
      }
    } yield ()

    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- cxnIO.transact(xa)
    } yield ()
  }
}
