package org.hyperledger.identus.sharedtest.containers

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.*
import doobie.implicits.*
import doobie.util.transactor.Transactor
import org.hyperledger.identus.shared.db.{ContextAwareTask, DbConfig}
import org.hyperledger.identus.shared.db.{ContextAwareTask, TransactorLayer}
import org.hyperledger.identus.shared.db.Implicits.*
import zio.*
import zio.interop.catz.*

trait PostgresTestContainerSupport {

  val appUser = "test-application-user"
  val appPassword = "password"

  protected val pgContainerLayer: TaskLayer[PostgreSQLContainer] = PostgresLayer.postgresLayer()

  protected val contextAwareTransactorLayer: RLayer[PostgreSQLContainer, Transactor[ContextAwareTask]] = {
    import doobie.*
    import doobie.implicits.*
    import zio.interop.catz.*

    val createAppUser = (xa: Transactor[Task]) =>
      //TODO: Refactor this to decouple the user creation from the Transactor initialization
      // it's important to know that these statement must be executed before the Flyway migrations
      doobie.free.connection.createStatement
        .map { stm =>
          stm.execute(s"""CREATE USER "$appUser" WITH PASSWORD '$appPassword';""")
          stm.execute(
            s"""ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "$appUser";"""
          )
          stm
        }
        .transact(xa)
        .unit

    val superUserTransactor = ZLayer.makeSome[PostgreSQLContainer, Transactor[Task]](
      TransactorLayer.task,
      PostgresLayer.dbConfigLayer,
    )

    val appUserTransactor = ZLayer.makeSome[PostgreSQLContainer, Transactor[ContextAwareTask]](
      TransactorLayer.contextAwareTask,
      PostgresLayer.dbConfigLayer.map(conf =>
        ZEnvironment(
          conf.get.copy(
            username = appUser,
            password = appPassword
          )
        )
      ),
    )

    val initializedTransactor = ZLayer.fromZIO {
      for {
        _ <- ZIO
          .serviceWithZIO[Transactor[Task]](createAppUser)
          .provideSomeLayer(superUserTransactor)
      } yield appUserTransactor
    }.flatten

    initializedTransactor
  }

  protected val systemTransactorLayer: RLayer[PostgreSQLContainer, Transactor[Task]] = {
    PostgresLayer.dbConfigLayer >+> TransactorLayer.task
  }
}
