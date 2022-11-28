package io.iohk.atala.agent.server

import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.server.http.{HttpRoutes, HttpServer, ZHttp4sBlazeServer, ZHttpEndpoints}
import io.iohk.atala.castor.core.service.{DIDService, DIDServiceImpl}
import io.iohk.atala.agent.server.http.marshaller.{
  DIDApiMarshallerImpl,
  DIDAuthenticationApiMarshallerImpl,
  DIDOperationsApiMarshallerImpl,
  DIDRegistrarApiMarshallerImpl,
  IssueCredentialsApiMarshallerImpl,
  ConnectionsManagementApiMarshallerImpl
}
import io.iohk.atala.agent.server.http.service.{
  DIDApiServiceImpl,
  DIDAuthenticationApiServiceImpl,
  DIDOperationsApiServiceImpl,
  DIDRegistrarApiServiceImpl,
  IssueCredentialsApiServiceImpl,
  ConnectionsManagementApiServiceImpl
}
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import io.iohk.atala.agent.openapi.api.{
  DIDApi,
  DIDAuthenticationApi,
  DIDOperationsApi,
  DIDRegistrarApi,
  IssueCredentialsApi,
  ConnectionsManagementApi
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
import io.iohk.atala.castor.sql.repository.DbConfig as CastorDbConfig
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.sql.repository.JdbcCredentialRepository
import io.iohk.atala.pollux.sql.repository.{DbConfig => PolluxDbConfig}
import io.iohk.atala.connect.sql.repository.{DbConfig => ConnectDbConfig}
import io.iohk.atala.agent.server.jobs.*
import zio.*
import zio.config.typesafe.TypesafeConfigSource
import zio.config.{ReadError, read}
import zio.interop.catz.*
import zio.stream.ZStream
import zhttp.http.*
import zhttp.service.Server

import java.util.concurrent.Executors
import io.iohk.atala.mercury.*
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.model.error.*
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.pollux.core.model.error.IssueCredentialError
import io.iohk.atala.pollux.core.model.error.IssueCredentialError.RepositoryError

import java.io.IOException
import cats.implicits.*
import io.iohk.atala.pollux.schema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.service.SchemaRegistryServiceInMemory
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.connect.core.service.ConnectionServiceImpl
import io.iohk.atala.connect.core.repository.ConnectionRepository
import io.iohk.atala.connect.sql.repository.JdbcConnectionRepository
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse
import io.iohk.atala.connect.core.model.error.ConnectionError
import io.iohk.atala.pollux.schema.{SchemaRegistryServerEndpoints, VerificationPolicyServerEndpoints}
import io.iohk.atala.pollux.service.{SchemaRegistryServiceInMemory, VerificationPolicyServiceInMemory}

object Modules {

  def app(port: Int): RIO[DidComm, Unit] = {
    val httpServerApp = HttpRoutes.routes.flatMap(HttpServer.start(port, _))

    httpServerApp
      .provideLayer(SystemModule.actorSystemLayer ++ HttpModule.layers)
      .unit
  }

  lazy val zioApp = {
    val zioHttpServerApp = for {
      allSchemaRegistryEndpoints <- SchemaRegistryServerEndpoints.all
      allVerificationPolicyEndpoints <- VerificationPolicyServerEndpoints.all
      allEndpoints = ZHttpEndpoints.withDocumentations[Task](
        allSchemaRegistryEndpoints ++ allVerificationPolicyEndpoints
      )
      appConfig <- ZIO.service[AppConfig]
      httpServer <- ZHttp4sBlazeServer.start(allEndpoints, port = appConfig.agent.httpEndpoint.http.port)
    } yield httpServer

    zioHttpServerApp
      .provideLayer(
        SchemaRegistryServiceInMemory.layer ++ VerificationPolicyServiceInMemory.layer ++ SystemModule.configLayer
      )
      .unit
  }

  def didCommServiceEndpoint(port: Int) = {
    val header = "content-type" -> MediaTypes.contentTypeEncrypted
    val app: HttpApp[DidComm & CredentialService & ConnectionService, Throwable] =
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
            .map(str => Response.ok)

      }
    Server.start(port, app)
  }

  val didCommExchangesJob: RIO[DidComm, Unit] =
    BackgroundJobs.didCommExchanges
      .repeat(Schedule.spaced(10.seconds))
      .unit
      .provideSomeLayer(AppModule.credentialServiceLayer)

  val connectDidCommExchangesJob: RIO[DidComm, Unit] =
    ConnectBackgroundJobs.didCommExchanges
      .repeat(Schedule.spaced(10.seconds))
      .unit
      .provideSomeLayer(AppModule.connectionServiceLayer)

  def webServerProgram(
      jsonString: String
  ): ZIO[DidComm & CredentialService & ConnectionService, MercuryThrowable, Unit] = {
    import io.iohk.atala.mercury.DidComm.*
    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        msg <- unpack(jsonString).map(_.getMessage)
        credentialService <- ZIO.service[CredentialService]
        connectionService <- ZIO.service[ConnectionService]
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
              } yield ()

            case s if s == OfferCredential.`type` => // Holder
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Holder in issue-credential:")
                _ <- ZIO.logInfo("Got OfferCredential: " + msg)
                offerFromIssuer = OfferCredential.readFromMessage(msg)
                _ <- credentialService
                  .receiveCredentialOffer(offerFromIssuer)
                  .catchSome { case RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }

              } yield ()

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
              } yield ()

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
              } yield ()

            case s if s == ConnectionRequest.`type` =>
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Inviter in connect:")
                connectionRequest = ConnectionRequest.readFromMessage(msg)
                _ <- ZIO.logInfo("Got ConnectionRequest: " + connectionRequest)
                // Receive and store ConnectionRequest
                maybeRecord <- connectionService
                  .receiveConnectionRequest(connectionRequest)
                  .catchSome { case ConnectionError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
                // Accept the ConnectionRequest
                _ <- connectionService
                  .acceptConnectionRequest(maybeRecord.get.id) // TODO: get
                  .catchSome { case ConnectionError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            // As an Invitee, I received a ConnectionResponse from an Inviter who replied to my ConnectionRequest.
            case s if s == ConnectionResponse.`type` =>
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Invitee in connect:")
                connectionResponse = ConnectionResponse.readFromMessage(msg)
                _ <- ZIO.logInfo("Got ConnectionResponse: " + connectionResponse)
                _ <- connectionService
                  .receiveConnectionResponse(connectionResponse)
                  .catchSome { case ConnectionError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            case _ => ZIO.succeed("Unknown Message Type")
          }
        }
      } yield ()
    }
  }

  val publishCredentialsToDltJob: RIO[DidComm, Unit] = {
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

  val connectionServiceLayer: RLayer[DidComm, ConnectionService] =
    (GrpcModule.layers ++ RepoModule.layers) >>> ConnectionServiceImpl.layer
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

  val connectionsManagementApiLayer: RLayer[DidComm, ConnectionsManagementApi] = {
    val serviceLayer = AppModule.connectionServiceLayer
    val apiServiceLayer = serviceLayer >>> ConnectionsManagementApiServiceImpl.layer
    val apiMarshallerLayer = ConnectionsManagementApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new ConnectionsManagementApi(_, _))
  }

  val layers =
    didApiLayer ++ didOperationsApiLayer ++ didAuthenticationApiLayer ++ didRegistrarApiLayer ++ issueCredentialsApiLayer ++ issueCredentialsProtocolApiLayer ++ connectionsManagementApiLayer
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

  val connectDbConfigLayer: TaskLayer[ConnectDbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.connect.database) map { config =>
        ConnectDbConfig(
          username = config.username,
          password = config.password,
          jdbcUrl = s"jdbc:postgresql://${config.host}:${config.port}/${config.databaseName}",
          awaitConnectionThreads = 2
        )
      }
    }
    SystemModule.configLayer >>> dbConfigLayer
  }

  val connectTransactorLayer: TaskLayer[Transactor[Task]] = {
    val transactorLayer = ZLayer.fromZIO {
      ZIO.service[ConnectDbConfig].flatMap { config =>
        Dispatcher[Task].allocated.map { case (dispatcher, _) =>
          given Dispatcher[Task] = dispatcher
          io.iohk.atala.connect.sql.repository.TransactorLayer.hikari[Task](config)
        }
      }
    }.flatten
    connectDbConfigLayer >>> transactorLayer
  }

  val didOperationRepoLayer: TaskLayer[DIDOperationRepository[Task]] =
    castorTransactorLayer >>> JdbcDIDOperationRepository.layer

  val credentialRepoLayer: TaskLayer[CredentialRepository[Task]] =
    polluxTransactorLayer >>> JdbcCredentialRepository.layer

  val connectionRepoLayer: TaskLayer[ConnectionRepository[Task]] =
    connectTransactorLayer >>> JdbcConnectionRepository.layer

  val layers = didOperationRepoLayer ++ credentialRepoLayer ++ connectionRepoLayer
}
