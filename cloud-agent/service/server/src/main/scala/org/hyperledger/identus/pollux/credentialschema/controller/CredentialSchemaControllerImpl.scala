package org.hyperledger.identus.pollux.credentialschema.controller

import cats.implicits.*
import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.walletapi.model.{ManagedDIDState, PublicationState}
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.api.http.*
import org.hyperledger.identus.api.http.model.{CollectionStats, Order, Pagination}
import org.hyperledger.identus.castor.core.model.did.{LongFormPrismDID, PrismDID}
import org.hyperledger.identus.pollux.core
import org.hyperledger.identus.pollux.core.model
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema.FilteredEntries
import org.hyperledger.identus.pollux.core.model.ResourceResolutionMethod
import org.hyperledger.identus.pollux.core.service.CredentialSchemaService
import org.hyperledger.identus.pollux.credentialschema.http.{
  CredentialSchemaDidUrlResponse,
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage,
  FilterInput
}
import org.hyperledger.identus.pollux.credentialschema.http.CredentialSchemaInput.toDomain
import org.hyperledger.identus.pollux.credentialschema.http.CredentialSchemaResponse.fromDomain
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*
import zio.json.*
import zio.json.ast.Json

import java.util.UUID
import scala.language.implicitConversions

class CredentialSchemaControllerImpl(service: CredentialSchemaService, managedDIDService: ManagedDIDService)
    extends CredentialSchemaController {
  override def createSchema(
      in: CredentialSchemaInput
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponse] = {
    for {
      _ <- validatePrismDID(in.author)
      result <- service
        .create(toDomain(in))
        .map(cs => fromDomain(cs).withBaseUri(rc.request.uri))
    } yield result
  }

  def createSchemaDidUrl(config: AppConfig, in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaDidUrlResponse] = {
    val res = for {
      validated <- validatePrismDID(in.author)
      serviceName = config.agent.httpEndpoint.serviceName
      result <- service.create(toDomain(in), ResourceResolutionMethod.DID)
      response <- ZIO
        .fromEither(CredentialSchemaDidUrlResponse.fromDomain(result, serviceName))
        .mapError(e =>
          ErrorResponse.internalServerError(detail = Some(s"Error occurred while parsing a schema response: $e"))
        )

    } yield response

    res
  }

  override def updateSchema(id: UUID, in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponse | CredentialSchemaDidUrlResponse] = {
    val res = for {
      _ <- validatePrismDID(in.author)
      cs <- service
        .update(id, toDomain(in))
      result <- cs.resolutionMethod match
        case ResourceResolutionMethod.DID =>
          ZIO
            .fromEither(CredentialSchemaDidUrlResponse.fromDomain(cs, ""))
            .mapError(e =>
              ErrorResponse.internalServerError(detail = Some(s"Error occurred while parsing a schema response: $e"))
            )

        case ResourceResolutionMethod.HTTP =>
          ZIO.succeed(fromDomain(cs).withBaseUri(rc.request.uri))
    } yield result

    res
  }

  override def getSchemaByGuid(config: AppConfig, guid: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponse | CredentialSchemaDidUrlResponse] = {
    val res: IO[ErrorResponse, CredentialSchemaResponse | CredentialSchemaDidUrlResponse] = for {
      cs <- service.getByGUID(guid)
      serviceName = config.agent.httpEndpoint.serviceName
      response <- cs.resolutionMethod match
        case model.ResourceResolutionMethod.DID =>
          ZIO
            .fromEither(CredentialSchemaDidUrlResponse.fromDomain(cs, serviceName))
            .mapError(e =>
              ErrorResponse.internalServerError(detail = Some(s"Error occurred while parsing a schema response: $e"))
            )
        case model.ResourceResolutionMethod.HTTP => ZIO.succeed(fromDomain(cs).withSelf(rc.request.uri.toString))
    } yield response

    res
  }

  override def getSchemaJsonByGuid(guid: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, Json] = {
    service
      .getByGUID(guid)
      .map(
        _.schema
      )
  }

  override def delete(guid: UUID)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponse] = {
    service
      .delete(guid)
      .map(
        fromDomain(_)
          .withBaseUri(rc.request.uri)
      )
  }

  override def lookupSchemas(
      filter: FilterInput,
      pagination: Pagination,
      order: Option[Order],
      config: AppConfig
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponsePage] = {
    for {
      filteredEntries: FilteredEntries <- service.lookup(
        filter.toDomain,
        pagination.offset,
        pagination.limit
      )
      serviceName = config.agent.httpEndpoint.serviceName
      entriesZio = filteredEntries.entries
        .traverse(cs =>
          cs.resolutionMethod match {
            case ResourceResolutionMethod.DID =>
              CredentialSchemaDidUrlResponse.fromDomain(cs, serviceName).flatMap(_.toJsonAST)
            case ResourceResolutionMethod.HTTP =>
              fromDomain(cs).withBaseUri(rc.request.uri).toJsonAST

          }
        )

      entries <- ZIO
        .fromEither(entriesZio)
        .mapError(e =>
          ErrorResponse.internalServerError(detail = Some(s"Error occurred while parsing a schema response: $e"))
        )

      page = CredentialSchemaResponsePage(entries)
      stats = CollectionStats(filteredEntries.totalCount, filteredEntries.count)
    } yield CredentialSchemaControllerLogic(rc, pagination, page, stats).result
  }

  private def validatePrismDID(author: String) =
    for {
      authorDID <- ZIO
        .fromEither(PrismDID.fromString(author))
        .mapError(ex => ErrorResponse.badRequest(detail = Some(s"Unable to parse Prism DID from '${author}' due: $ex")))
      longFormPrismDID <- getLongForm(authorDID, true)
    } yield longFormPrismDID

  private def getLongForm(
      did: PrismDID,
      allowUnpublishedIssuingDID: Boolean = false
  ): ZIO[WalletAccessContext, ErrorResponse, LongFormPrismDID] = {
    for {
      didState <- managedDIDService
        .getManagedDIDState(did.asCanonical)
        .mapError(e =>
          ErrorResponse.internalServerError(detail =
            Some(s"Error occurred while getting DID from wallet: ${e.toString}")
          )
        )
        .someOrFail(ErrorResponse.notFound(detail = Some(s"Issuer DID does not exist in the wallet: $did")))
        .flatMap {
          case s @ ManagedDIDState(_, _, _: PublicationState.Published) => ZIO.succeed(s)
          case s =>
            ZIO.cond(
              allowUnpublishedIssuingDID,
              s,
              ErrorResponse.badRequest(detail = Some(s"Issuer DID must be published: $did"))
            )
        }
      longFormPrismDID = PrismDID.buildLongFormFromOperation(didState.createOperation)
    } yield longFormPrismDID
  }
}

object CredentialSchemaControllerImpl {
  val layer: URLayer[CredentialSchemaService & ManagedDIDService, CredentialSchemaController] =
    ZLayer.fromFunction(CredentialSchemaControllerImpl(_, _))
}
