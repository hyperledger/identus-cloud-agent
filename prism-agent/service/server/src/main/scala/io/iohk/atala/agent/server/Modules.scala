package io.iohk.atala.agent.server

import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.server.http.{HttpRoutes, HttpServer, ZHttp4sBlazeServer, ZHttpEndpoints}
import io.iohk.atala.castor.core.service.{DIDService, DIDServiceImpl}
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.agent.server.http.marshaller.{
  ConnectionsManagementApiMarshallerImpl,
  DIDApiMarshallerImpl,
  DIDRegistrarApiMarshallerImpl
}
import io.iohk.atala.agent.server.http.service.{ConnectionsManagementApiServiceImpl, DIDApiServiceImpl}
import io.iohk.atala.agent.openapi.api.{ConnectionsManagementApi, DIDApi, DIDRegistrarApi}
import cats.effect.std.Dispatcher
import com.typesafe.config.ConfigFactory
import doobie.util.transactor.Transactor
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.agent.openapi.api.*
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.walletapi.model.error.DIDSecretStorageError
import io.iohk.atala.agent.server.http.marshaller.*
import io.iohk.atala.agent.server.http.service.*
import io.iohk.atala.agent.server.http.{HttpRoutes, HttpServer}
import io.iohk.atala.pollux.core.service.{
  CredentialSchemaService,
  CredentialSchemaServiceImpl,
  CredentialService,
  CredentialServiceImpl,
  PresentationService,
  PresentationServiceImpl,
  VerificationPolicyService,
  VerificationPolicyServiceImpl
}
import io.iohk.atala.pollux.credentialschema.controller.{CredentialSchemaController, CredentialSchemaControllerImpl}
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.sql.repository.{
  JdbcCredentialRepository,
  JdbcCredentialSchemaRepository,
  JdbcPresentationRepository,
  JdbcVerificationPolicyRepository,
  DbConfig as PolluxDbConfig
}
import io.iohk.atala.connect.sql.repository.DbConfig as ConnectDbConfig
import io.iohk.atala.agent.server.sql.DbConfig as AgentDbConfig
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
import io.iohk.atala.mercury.DidOps.*
import io.iohk.atala.mercury.model.*
import io.iohk.atala.mercury.model.error.*
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.pollux.core.model.error.CredentialServiceError.RepositoryError
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc

import java.io.IOException
import cats.implicits.*
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.core.repository.PresentationRepository
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.connect.core.service.ConnectionServiceImpl
import io.iohk.atala.connect.core.repository.ConnectionRepository
import io.iohk.atala.connect.sql.repository.JdbcConnectionRepository
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.pollux.credentialschema.{SchemaRegistryServerEndpoints, VerificationPolicyServerEndpoints}
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.mercury.protocol.presentproof.*
import io.iohk.atala.agent.server.config.AgentConfig
import org.didcommx.didcomm.DIDComm
import io.iohk.atala.resolvers.UniversalDidResolver
import org.didcommx.didcomm.secret.SecretResolver
import org.didcommx.didcomm.model.UnpackParams
import org.didcommx.didcomm.secret.Secret
import io.circe.ParsingFailure
import io.circe.DecodingFailure
import io.iohk.atala.agent.walletapi.sql.{JdbcDIDNonSecretStorage, JdbcDIDSecretStorage}
import io.iohk.atala.resolvers.DIDResolver
import io.iohk.atala.pollux.vc.jwt.DidResolver as JwtDidResolver
import io.iohk.atala.pollux.vc.jwt.PrismDidResolver
import io.iohk.atala.mercury.DidAgent
import io.iohk.atala.pollux.credentialschema.controller.{
  CredentialSchemaController,
  VerificationPolicyController,
  VerificationPolicyControllerImpl,
  VerificationPolicyControllerInMemory
}

object Modules {

  def app(port: Int): RIO[
    DidOps & DidAgent & ManagedDIDService & AppConfig & DIDRegistrarApi & IssueCredentialsProtocolApi &
      ConnectionsManagementApi & DIDApi & PresentProofApi & ActorSystem[Nothing],
    Unit
  ] = {
    val httpServerApp = HttpRoutes.routes.flatMap(HttpServer.start(port, _))

    httpServerApp.unit
  }

  lazy val zioApp: RIO[CredentialSchemaController & VerificationPolicyController & AppConfig, Unit] = {
    val zioHttpServerApp = for {
      allSchemaRegistryEndpoints <- SchemaRegistryServerEndpoints.all
      allVerificationPolicyEndpoints <- VerificationPolicyServerEndpoints.all
      allEndpoints = ZHttpEndpoints.withDocumentations[Task](
        allSchemaRegistryEndpoints ++ allVerificationPolicyEndpoints
      )
      appConfig <- ZIO.service[AppConfig]
      httpServer <- ZHttp4sBlazeServer.start(allEndpoints, port = appConfig.agent.httpEndpoint.http.port)
    } yield httpServer

    zioHttpServerApp.unit
  }

  def didCommServiceEndpoint(port: Int) = {
    val header = "content-type" -> MediaTypes.contentTypeEncrypted
    val app: HttpApp[
      DidOps & DidAgent & CredentialService & PresentationService & ConnectionService & ManagedDIDService,
      Throwable
    ] =
      Http.collectZIO[Request] {
        //   // TODO add DIDComm messages parsing logic here!
        //   Response.text("Hello World!").setStatus(Status.Accepted)
        // case Method.POST -> !! / "did-comm-v2" =>
        case Method.GET -> !! / "did" =>
          for {
            didCommService <- ZIO.service[DidAgent]
            str = didCommService.id.value
          } yield (Response.text(str))

        case req @ Method.POST -> !!
            if req.headersAsList.exists(h => h._1.equalsIgnoreCase(header._1) && h._2.equalsIgnoreCase(header._2)) =>
          val result = for {
            data <- req.body.asString
            _ <- webServerProgram(data)
          } yield Response.ok

          result
            .tapError { error =>
              ZIO.logErrorCause("Fail to POST form webServerProgram", Cause.fail(error))
            }
            .mapError {
              case ex: DIDSecretStorageError => ex
              case ex: MercuryThrowable      => mercuryErrorAsThrowable(ex)
            }
      }
    Server.start(port, app)
  }

  val issueCredentialDidCommExchangesJob: RIO[
    AppConfig & DidOps & DIDResolver & JwtDidResolver & HttpClient & CredentialService & DIDService &
      ManagedDIDService & PresentationService,
    Unit
  ] =
    for {
      config <- ZIO.service[AppConfig]
      job <- BackgroundJobs.issueCredentialDidCommExchanges
        .repeat(Schedule.spaced(config.pollux.issueBgJobRecurrenceDelay))
        .unit
    } yield job

  val presentProofExchangeJob: RIO[
    AppConfig & DidOps & DIDResolver & JwtDidResolver & HttpClient & PresentationService & CredentialService &
      DIDService & ManagedDIDService,
    Unit
  ] =
    for {
      config <- ZIO.service[AppConfig]
      job <- BackgroundJobs.presentProofExchanges
        .repeat(Schedule.spaced(config.pollux.presentationBgJobRecurrenceDelay))
        .unit
    } yield job

  val connectDidCommExchangesJob
      : RIO[AppConfig & DidOps & DIDResolver & HttpClient & ConnectionService & ManagedDIDService, Unit] =
    for {
      config <- ZIO.service[AppConfig]
      job <- ConnectBackgroundJobs.didCommExchanges
        .repeat(Schedule.spaced(config.connect.connectBgJobRecurrenceDelay))
        .unit
    } yield job

  val syncDIDPublicationStateFromDltJob: URIO[ManagedDIDService, Unit] =
    BackgroundJobs.syncDIDPublicationStateFromDlt
      .catchAll(e => ZIO.logError(s"error while syncing DID publication state: $e"))
      .repeat(Schedule.spaced(10.seconds))
      .unit

  private[this] def extractFirstRecipientDid(jsonMessage: String): IO[ParsingFailure | DecodingFailure, String] = {
    import io.circe._, io.circe.parser._
    val doc = parse(jsonMessage).getOrElse(Json.Null)
    val cursor = doc.hcursor
    ZIO.fromEither(
      cursor.downField("recipients").downArray.downField("header").downField("kid").as[String].map(_.split("#")(0))
    )
  }

  private[this] def unpackMessage(
      jsonString: String
  ): ZIO[DidOps & ManagedDIDService, ParseResponse | DIDSecretStorageError, Message] = {
    // Needed for implicit conversion from didcommx UnpackResuilt to mercury UnpackMessage
    import io.iohk.atala.mercury.model.given
    for {
      recipientDid <- extractFirstRecipientDid(jsonString).mapError(err => ParseResponse(err))
      _ <- ZIO.logInfo(s"Extracted recipient Did => $recipientDid")
      managedDIDService <- ZIO.service[ManagedDIDService]
      peerDID <- managedDIDService.getPeerDID(DidId(recipientDid))
      agent = AgentPeerService.makeLayer(peerDID)
      msg <- unpack(jsonString).provideSomeLayer(agent)
    } yield msg.message
  }

  def webServerProgram(
      jsonString: String
  ): ZIO[
    DidOps & CredentialService & PresentationService & ConnectionService & ManagedDIDService,
    MercuryThrowable | DIDSecretStorageError,
    Unit
  ] = {

    ZIO.logAnnotate("request-id", java.util.UUID.randomUUID.toString()) {
      for {
        _ <- ZIO.logInfo("Received new message")
        _ <- ZIO.logTrace(jsonString)
        msg <- unpackMessage(jsonString)
        credentialService <- ZIO.service[CredentialService]
        connectionService <- ZIO.service[ConnectionService]
        _ <- {
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
                  .catchSome { case CredentialServiceError.RepositoryError(cause) =>
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
                  .catchSome { case CredentialServiceError.RepositoryError(cause) =>
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
                  .catchSome { case CredentialServiceError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            // #####################
            // ### present-proof ###
            // #####################

            case s if s == ProposePresentation.`type` =>
              for {
                _ <- ZIO.unit
                request = ProposePresentation.readFromMessage(msg)
                _ <- ZIO.logInfo("As a Verifier in  present-proof got ProposePresentation: " + request)
                service <- ZIO.service[PresentationService]
                _ <- service
                  .receiveProposePresentation(request)
                  .catchSome { case PresentationError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *> ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            case s if s == RequestPresentation.`type` =>
              for {
                _ <- ZIO.unit
                request = RequestPresentation.readFromMessage(msg)
                _ <- ZIO.logInfo("As a Prover in present-proof got RequestPresentation: " + request)
                service <- ZIO.service[PresentationService]
                _ <- service
                  .receiveRequestPresentation(None, request)
                  .catchSome { case PresentationError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *> ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()
            case s if s == Presentation.`type` =>
              for {
                _ <- ZIO.unit
                request = Presentation.readFromMessage(msg)
                _ <- ZIO.logInfo("As a Verifier in present-proof got Presentation: " + request)
                service <- ZIO.service[PresentationService]
                _ <- service
                  .receivePresentation(request)
                  .catchSome { case PresentationError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *> ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
              } yield ()

            case s if s == ConnectionRequest.`type` =>
              for {
                _ <- ZIO.logInfo("*" * 100)
                _ <- ZIO.logInfo("As an Inviter in connect:")
                connectionRequest <- ConnectionRequest.fromMessage(msg) match {
                  case Left(error)  => ZIO.fail(new RuntimeException(error))
                  case Right(value) => ZIO.succeed(value)
                }
                _ <- ZIO.logInfo("Got ConnectionRequest: " + connectionRequest)
                // Receive and store ConnectionRequest
                maybeRecord <- connectionService
                  .receiveConnectionRequest(connectionRequest)
                  .catchSome { case ConnectionServiceError.RepositoryError(cause) =>
                    ZIO.logError(cause.getMessage()) *>
                      ZIO.fail(cause)
                  }
                  .catchAll { case ex: IOException => ZIO.fail(ex) }
                // Accept the ConnectionRequest
                _ <- connectionService
                  .acceptConnectionRequest(maybeRecord.id)
                  .catchSome { case ConnectionServiceError.RepositoryError(cause) =>
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
                connectionResponse <- ConnectionResponse.fromMessage(msg) match {
                  case Left(error)  => ZIO.fail(new RuntimeException(error))
                  case Right(value) => ZIO.succeed(value)
                }
                _ <- ZIO.logInfo("Got ConnectionResponse: " + connectionResponse)
                _ <- connectionService
                  .receiveConnectionResponse(connectionResponse)
                  .catchSome { case ConnectionServiceError.RepositoryError(cause) =>
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

  val publishCredentialsToDltJob: RIO[CredentialService, Unit] = {
    val effect = BackgroundJobs.publishCredentialsToDlt
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
  val didOpValidatorLayer: ULayer[DIDOperationValidator] = DIDOperationValidator.layer()

  val didJwtResolverlayer: URLayer[DIDService, JwtDidResolver] =
    ZLayer.fromFunction(PrismDidResolver(_))

  val didServiceLayer: TaskLayer[DIDService] =
    (didOpValidatorLayer ++ GrpcModule.layers) >>> DIDServiceImpl.layer

  val manageDIDServiceLayer: TaskLayer[ManagedDIDService] = {
    val secretStorageLayer = RepoModule.agentTransactorLayer >>> JdbcDIDSecretStorage.layer
    val nonSecretStorageLayer = RepoModule.agentTransactorLayer >>> JdbcDIDNonSecretStorage.layer
    (didOpValidatorLayer ++ didServiceLayer ++ secretStorageLayer ++ nonSecretStorageLayer) >>> ManagedDIDService.layer
  }

  val credentialServiceLayer: RLayer[DidOps & DidAgent & JwtDidResolver, CredentialService] =
    (GrpcModule.layers ++ RepoModule.credentialRepoLayer) >>> CredentialServiceImpl.layer

  def presentationServiceLayer =
    (RepoModule.presentationRepoLayer ++ RepoModule.credentialRepoLayer) >>> PresentationServiceImpl.layer

  val connectionServiceLayer: RLayer[DidOps & DidAgent, ConnectionService] =
    (GrpcModule.layers ++ RepoModule.connectionRepoLayer) >>> ConnectionServiceImpl.layer

}

object GrpcModule {
  // TODO: once Castor + Pollux has migrated to use Node 2.0 stubs, this should be removed.
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

  val prismNodeStubLayer: TaskLayer[NodeServiceGrpc.NodeServiceStub] = {
    val stubLayer = ZLayer.fromZIO(
      ZIO
        .service[AppConfig]
        .map(_.prismNode.service)
        .flatMap(config =>
          ZIO.attempt(
            NodeServiceGrpc.stub(ManagedChannelBuilder.forAddress(config.host, config.port).usePlaintext.build)
          )
        )
    )
    SystemModule.configLayer >>> stubLayer
  }

  val layers = irisStubLayer ++ prismNodeStubLayer
}

object HttpModule {
  val didApiLayer: TaskLayer[DIDApi] = {
    val serviceLayer = AppModule.didServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDApiServiceImpl.layer
    val apiMarshallerLayer = DIDApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDApi(_, _))
  }

  val didRegistrarApiLayer: TaskLayer[DIDRegistrarApi] = {
    val serviceLayer = AppModule.manageDIDServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDRegistrarApiServiceImpl.layer
    val apiMarshallerLayer = DIDRegistrarApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDRegistrarApi(_, _))
  }

  val issueCredentialsProtocolApiLayer: RLayer[
    DidOps & DidAgent & ManagedDIDService & ConnectionService & AppConfig & JwtDidResolver,
    IssueCredentialsProtocolApi
  ] = {
    val serviceLayer = AppModule.credentialServiceLayer
    val apiServiceLayer = serviceLayer >>> IssueCredentialsProtocolApiServiceImpl.layer
    val apiMarshallerLayer = IssueCredentialsProtocolApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new IssueCredentialsProtocolApi(_, _))
  }

  val presentProofProtocolApiLayer: RLayer[DidOps & DidAgent, PresentProofApi] = {
    val serviceLayer = AppModule.presentationServiceLayer ++ AppModule.connectionServiceLayer // ++ didCommServiceLayer
    val apiServiceLayer = serviceLayer >>> PresentProofApiServiceImpl.layer
    val apiMarshallerLayer = PresentProofApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new PresentProofApi(_, _))
  }

  val connectionsManagementApiLayer
      : RLayer[DidOps & DidAgent & ManagedDIDService & AppConfig, ConnectionsManagementApi] = {
    val serviceLayer = AppModule.connectionServiceLayer
    val apiServiceLayer = serviceLayer >>> ConnectionsManagementApiServiceImpl.layer
    val apiMarshallerLayer = ConnectionsManagementApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new ConnectionsManagementApi(_, _))
  }

  val layers =
    didApiLayer ++ didRegistrarApiLayer ++
      issueCredentialsProtocolApiLayer ++ connectionsManagementApiLayer ++ presentProofProtocolApiLayer
}

object RepoModule {

  val polluxDbConfigLayer: TaskLayer[PolluxDbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.pollux.database) map { config =>
        PolluxDbConfig(
          username = config.username,
          password = config.password,
          jdbcUrl = s"jdbc:postgresql://${config.host}:${config.port}/${config.databaseName}",
          awaitConnectionThreads = config.awaitConnectionThreads
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
          awaitConnectionThreads = config.awaitConnectionThreads
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

  val agentDbConfigLayer: TaskLayer[AgentDbConfig] = {
    val dbConfigLayer = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.agent.database) map { config =>
        AgentDbConfig(
          username = config.username,
          password = config.password,
          jdbcUrl = s"jdbc:postgresql://${config.host}:${config.port}/${config.databaseName}",
          awaitConnectionThreads = config.awaitConnectionThreads
        )
      }
    }
    SystemModule.configLayer >>> dbConfigLayer
  }

  val agentTransactorLayer: TaskLayer[Transactor[Task]] = {
    val transactorLayer = ZLayer.fromZIO {
      ZIO.service[AgentDbConfig].flatMap { config =>
        Dispatcher[Task].allocated.map { case (dispatcher, _) =>
          given Dispatcher[Task] = dispatcher
          io.iohk.atala.agent.server.sql.TransactorLayer.hikari[Task](config)
        }
      }
    }.flatten
    agentDbConfigLayer >>> transactorLayer
  }

  val credentialRepoLayer: TaskLayer[CredentialRepository[Task]] =
    RepoModule.polluxTransactorLayer >>> JdbcCredentialRepository.layer

  val presentationRepoLayer: TaskLayer[PresentationRepository[Task]] =
    polluxTransactorLayer >>> JdbcPresentationRepository.layer

  val connectionRepoLayer: TaskLayer[ConnectionRepository[Task]] =
    RepoModule.connectTransactorLayer >>> JdbcConnectionRepository.layer

  val credentialSchemaServiceLayer: TaskLayer[CredentialSchemaController] =
    RepoModule.polluxTransactorLayer >>>
      JdbcCredentialSchemaRepository.layer >>>
      CredentialSchemaServiceImpl.layer >>>
      CredentialSchemaControllerImpl.layer

  val verificationPolicyServiceLayer: TaskLayer[VerificationPolicyController] =
    RepoModule.polluxTransactorLayer >>>
      JdbcVerificationPolicyRepository.layer >+>
      VerificationPolicyServiceImpl.layer >+>
      VerificationPolicyControllerImpl.layer
}
