package io.iohk.atala.pollux.sql.repository

import cats.effect.{Async, Resource}
import doobie.util.transactor.Transactor
import com.zaxxer.hikari.HikariConfig
import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import zio.interop.catz.*
import zio.*
import cats.effect.std.Dispatcher

case class DbConfig(
    username: String,
    password: String,
    jdbcUrl: String,
    awaitConnectionThreads: Int = 8
)

object TransactorLayer {

  def hikari[A[_]: Async: Dispatcher](config: DbConfig)(using tag: Tag[Transactor[A]]): TaskLayer[Transactor[A]] = {
    val transactorLayerZio = ZIO
      .attempt {
        // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
        val poolSize = (config.awaitConnectionThreads * 2) + 1
        val hikariConfig = makeHikariConfig(config)
        hikariConfig.setPoolName("DBPool")
        hikariConfig.setLeakDetectionThreshold(300000) // 5 mins
        hikariConfig.setMinimumIdle(poolSize)
        hikariConfig.setMaximumPoolSize(poolSize) // Both Pool size amd Minimum Idle should same and is recommended
        hikariConfig
      }
      .map { hikariConfig =>
        val pool: Resource[A, Transactor[A]] = for {
          // Resource yielding a transactor configured with a bounded connect EC and an unbounded
          // transaction EC. Everything will be closed and shut down cleanly after use.
          ec <- ExecutionContexts.fixedThreadPool[A](config.awaitConnectionThreads) // our connect EC
          xa <- HikariTransactor.fromHikariConfig[A](hikariConfig, ec)
        } yield xa

        pool.toManaged.toLayer[Transactor[A]]
      }

    ZLayer.fromZIO(transactorLayerZio).flatten
  }

  private def makeHikariConfig(config: DbConfig): HikariConfig = {
    val hikariConfig = HikariConfig()

    hikariConfig.setJdbcUrl(config.jdbcUrl)
    hikariConfig.setUsername(config.username)
    hikariConfig.setPassword(config.password)
    hikariConfig.setAutoCommit(false)

    hikariConfig.setDriverClassName("org.postgresql.Driver")
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

    hikariConfig
  }

}
