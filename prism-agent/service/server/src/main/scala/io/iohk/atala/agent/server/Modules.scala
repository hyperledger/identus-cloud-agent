package io.iohk.atala.agent.server

import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.server.http.{HttpRoutes, HttpServer}
import io.iohk.atala.castor.core.service.{DIDService, DIDServiceImpl}
import io.iohk.atala.agent.server.http.marshaller.{
  DIDApiMarshallerImpl,
  DIDAuthenticationApiMarshallerImpl,
  DIDOperationsApiMarshallerImpl,
  DIDRegistrarApiMarshallerImpl,
  IssueCredentialsApiMarshallerImpl
}
import io.iohk.atala.agent.server.http.service.{
  DIDApiServiceImpl,
  DIDAuthenticationApiServiceImpl,
  DIDOperationsApiServiceImpl,
  DIDRegistrarApiServiceImpl,
  IssueCredentialsApiServiceImpl
}
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import io.iohk.atala.agent.openapi.api.{
  DIDApi,
  DIDAuthenticationApi,
  DIDOperationsApi,
  DIDRegistrarApi,
  IssueCredentialsApi
}
import io.iohk.atala.castor.sql.repository.{JdbcDIDOperationRepository, TransactorLayer}
import zio.*
import zio.interop.catz.*
import cats.effect.std.Dispatcher
import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.agent.openapi.api.*
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.server.http.marshaller.*
import io.iohk.atala.agent.server.http.service.*
import io.iohk.atala.agent.server.http.{HttpRoutes, HttpServer}
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import io.iohk.atala.castor.core.service.{DIDService, DIDServiceImpl}
import io.iohk.atala.pollux.core.service.CredentialServiceImpl
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.castor.sql.repository.{JdbcDIDOperationRepository, TransactorLayer}
import io.iohk.atala.castor.sql.repository.{DbConfig => CastorDbConfig}
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.sql.repository.JdbcCredentialRepository
import io.iohk.atala.pollux.sql.repository.{DbConfig => PolluxDbConfig}
import io.iohk.atala.agent.server.jobs.*
import zio.*
import zio.config.typesafe.TypesafeConfigSource
import zio.config.{ReadError, read}
import zio.interop.catz.*
import zio.stream.ZStream
import zhttp.http._
import zhttp.service.Server

import java.util.concurrent.Executors

import io.iohk.atala.mercury._
import io.iohk.atala.mercury.AgentCli.sendMessage //TODO REMOVE
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.issuecredential._
import io.iohk.atala.pollux.core.model.error.IssueCredentialError
import io.iohk.atala.pollux.core.model.error.IssueCredentialError.RepositoryError
import java.io.IOException

object Modules {

  def app(port: Int): RIO[DidComm, Unit] = {
    val httpServerApp = HttpRoutes.routes.flatMap(HttpServer.start(port, _))

    httpServerApp
      .provideLayer(SystemModule.actorSystemLayer ++ HttpModule.layers)
      .unit
  }

  def didCommServiceEndpoint(port: Int) = {
    val header = "content-type" -> MediaTypes.contentTypeEncrypted
    val app: HttpApp[DidComm with CredentialService, Throwable] =
      Http.collectZIO[Request] {
        //   // TODO add DIDComm messages parsing logic here!
        //   Response.text("Hello World!").setStatus(Status.Accepted)
        // case Method.POST -> !! / "did-comm-v2" =>
        case Method.GET -> !! / "did" =>
          for {
            didCommService <- ZIO.service[DidComm]
            str = didCommService.myDid.value
          } yield (Response.text(str))

        case req @ Method.POST -> !!
            if req.headersAsList.exists(h => h._1.equalsIgnoreCase(header._1) && h._2.equalsIgnoreCase(header._2)) =>
          req.body.asString
            // .catchNonFatalOrDie(ex => ZIO.fail(ParseResponse(ex)))
            .flatMap { data =>
              webServerProgram(data).catchAll { case ex =>
                val error = mercuryErrorAsThrowable(ex)
                ZIO.logErrorCause("Fail to POST form webServerProgram", Cause.fail(error)) *>
                  ZIO.fail(error)
              }
            }
            .map(str => Response.text(str))

      }
    Server.start(port, app)
  }

  val didCommExchangesJob: RIO[DidComm, Unit] =
    BackgroundJobs.didCommExchanges
      .repeat(Schedule.spaced(10.seconds))
      .unit
      .provideSomeLayer(AppModule.credentialServiceLayer)

  def webServerProgram(
      jsonString: String
  ): ZIO[DidComm with CredentialService, MercuryThrowable, String] = {
    import io.iohk.atala.mercury.DidComm.*
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        msg <- unpack(jsonString).map(_.getMessage)
        ret <- {
          msg.piuri match {
            // ########################
            // ### issue-credential ###
            // ########################
            case s if s == ProposeCredential.`type` => // Issuer
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Issuer in issue-credential:")
                _ <- ZIO.logInfo("Got ProposeCredential: " + msg)
                credentialService <- ZIO.service[CredentialService]

                // TODO
              } yield ("OfferCredential Sent")

            case s if s == OfferCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                _ <- ZIO.logInfo("Got OfferCredential: " + msg)
                credentialService <- ZIO.service[CredentialService]
                offerFromIssuer = OfferCredential.readFromMessage(msg)
                _ <- credentialService
                  .receiveCredentialOffer(offerFromIssuer)
                  .catchSome { case RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }

              } yield ("Offer received")

            case s if s == RequestCredential.`type` => // Issuer
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Issuer in issue-credential:")
                requestCredential = RequestCredential.readFromMessage(msg)
                _ <- ZIO.logInfo("Got RequestCredential: " + requestCredential)
                credentialService <- ZIO.service[CredentialService]
                todoTestOption <- credentialService
                  .receiveCredentialRequest(requestCredential)
                  .catchSome { case RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }

                // TODO todoTestOption if none
              } yield ("RequestCredential received")

            case s if s == IssueCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                issueCredential = IssueCredential.readFromMessage(msg)
                _ <- ZIO.logInfo("Got IssueCredential: " + issueCredential)
                credentialService <- ZIO.service[CredentialService]
                _ <- credentialService
                  .receiveCredentialIssue(issueCredential)
                  .catchSome { case RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }

              } yield ("IssueCredential Received")

            case _ => ZIO.succeed("Unknown Message Type")
          }
        }
      } yield (ret)
    }
  }

  val publishCredentialsToDltJob:  RIO[DidComm, Unit] = {
    val effect = BackgroundJobs.publishCredentialsToDlt
      .provideLayer(AppModule.credentialServiceLayer)
    (effect repeat Schedule.spaced(1.seconds)).unit
  }

}

object SystemModule {
  val actorSystemLayer: TaskLayer[ActorSystem[Nothing]] = ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.executor
        .map(_.asExecutionContext)
        .flatMap(ec =>
          ZIO.attempt(ActorSystem(Behaviors.empty, "actor-system", BootstrapSetup().withDefaultExecutionContext(ec)))
        )
    )(system => ZIO.attempt(system.terminate()).orDie)
  )

  val configLayer: Layer[ReadError[String], AppConfig] = ZLayer.fromZIO {
    read(
      AppConfig.descriptor.from(
        TypesafeConfigSource.fromTypesafeConfig(
          ZIO.attempt(ConfigFactory.load())
        )
      )
    )
  }
}

object AppModule {
  val didOpValidatorLayer: ULayer[DIDOperationValidator] = DIDOperationValidator.layer(
    DIDOperationValidator.Config(
      publicKeyLimit = 50,
      serviceLimit = 50
    )
  )

  val didServiceLayer: TaskLayer[DIDService] =
    (GrpcModule.layers ++ RepoModule.layers ++ didOpValidatorLayer) >>> DIDServiceImpl.layer

  val manageDIDServiceLayer: TaskLayer[ManagedDIDService] =
    (didOpValidatorLayer ++ didServiceLayer) >>> ManagedDIDService.inMemoryStorage()

  val credentialServiceLayer: RLayer[DidComm, CredentialService] =
    (GrpcModule.layers ++ RepoModule.layers) >>> CredentialServiceImpl.layer
}

object GrpcModule {
  val irisStubLayer: TaskLayer[IrisServiceStub] = {
    val stubLayer = ZLayer.fromZIO(
      ZIO
        .service[AppConfig]
        .map(_.iris.service)
        .flatMap(config =>
          ZIO.attempt(
            IrisServiceGrpc.stub(ManagedChannelBuilder.forAddress(config.host, config.port).usePlaintext.build)
          )
        )
    )
    SystemModule.configLayer >>> stubLayer
  }

  val layers = irisStubLayer
}

object HttpModule {
  val didApiLayer: TaskLayer[DIDApi] = {
    val serviceLayer = AppModule.didServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDApiServiceImpl.layer
    val apiMarshallerLayer = DIDApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDApi(_, _))
  }

  val didOperationsApiLayer: ULayer[DIDOperationsApi] = {
    val apiServiceLayer = DIDOperationsApiServiceImpl.layer
    val apiMarshallerLayer = DIDOperationsApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDOperationsApi(_, _))
  }

  val didAuthenticationApiLayer: ULayer[DIDAuthenticationApi] = {
    val apiServiceLayer = DIDAuthenticationApiServiceImpl.layer
    val apiMarshallerLayer = DIDAuthenticationApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDAuthenticationApi(_, _))
  }

  val didRegistrarApiLayer: TaskLayer[DIDRegistrarApi] = {
    val serviceLayer = AppModule.manageDIDServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDRegistrarApiServiceImpl.layer
    val apiMarshallerLayer = DIDRegistrarApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDRegistrarApi(_, _))
  }

  val issueCredentialsApiLayer: RLayer[DidComm, IssueCredentialsApi] = {
    val serviceLayer = AppModule.credentialServiceLayer
    val apiServiceLayer = serviceLayer >>> IssueCredentialsApiServiceImpl.layer
    val apiMarshallerLayer = IssueCredentialsApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new IssueCredentialsApi(_, _))
  }

  val issueCredentialsProtocolApiLayer: RLayer[DidComm, IssueCredentialsProtocolApi] = {
    val serviceLayer = AppModule.credentialServiceLayer
    val apiServiceLayer = serviceLayer >>> IssueCredentialsProtocolApiServiceImpl.layer
    val apiMarshallerLayer = IssueCredentialsProtocolApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new IssueCredentialsProtocolApi(_, _))
  }

  val layers =
    didApiLayer ++ didOperationsApiLayer ++ didAuthenticationApiLayer ++ didRegistrarApiLayer ++ issueCredentialsApiLayer ++ issueCredentialsProtocolApiLayer
}

object RepoModule {

  val castorDbConfigLayer: TaskLayer[CastorDbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.castor.database) map { config =>
        CastorDbConfig(
          username = config.username,
          password = config.password,
          jdbcUrl = s"jdbc:postgresql://${config.host}:${config.port}/${config.databaseName}"
        )
      }
    }
    SystemModule.configLayer >>> dbConfigLayer
  }

  val castorTransactorLayer: TaskLayer[Transactor[Task]] = {
    val transactorLayer = ZLayer.fromZIO {
      ZIO.service[CastorDbConfig].flatMap { config =>
        Dispatcher[Task].allocated.map { case (dispatcher, _) =>
          given Dispatcher[Task] = dispatcher
          TransactorLayer.hikari[Task](config)
        }
      }
    }.flatten
    castorDbConfigLayer >>> transactorLayer
  }

  val polluxDbConfigLayer: TaskLayer[PolluxDbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.pollux.database) map { config =>
        PolluxDbConfig(
          username = config.username,
          password = config.password,
          jdbcUrl = s"jdbc:postgresql://${config.host}:${config.port}/${config.databaseName}",
          awaitConnectionThreads = 2
        )
      }
    }
    SystemModule.configLayer >>> dbConfigLayer
  }

  val polluxTransactorLayer: TaskLayer[Transactor[Task]] = {
    val transactorLayer = ZLayer.fromZIO {
      ZIO.service[PolluxDbConfig].flatMap { config =>
        Dispatcher[Task].allocated.map { case (dispatcher, _) =>
          given Dispatcher[Task] = dispatcher
          io.iohk.atala.pollux.sql.repository.TransactorLayer.hikari[Task](config)
        }
      }
    }.flatten
    polluxDbConfigLayer >>> transactorLayer
  }

  val didOperationRepoLayer: TaskLayer[DIDOperationRepository[Task]] =
    castorTransactorLayer >>> JdbcDIDOperationRepository.layer

  val credentialRepoLayer: TaskLayer[CredentialRepository[Task]] =
    polluxTransactorLayer >>> JdbcCredentialRepository.layer

  val layers = didOperationRepoLayer ++ credentialRepoLayer
}
