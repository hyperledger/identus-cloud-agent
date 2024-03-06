package io.iohk.atala.iam.oidc.domain

import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.iam.oidc.http.CredentialDefinition
import zio.{IO, ULayer, ZIO, ZLayer}

trait CredentialIssuerService {

  import CredentialIssuerService.Error
  import CredentialIssuerService.Errors.*
  def verifyJwtProof(jwt: String): IO[InvalidProof, Boolean]

  def validateCredentialDefinition(
      credentialDefinition: CredentialDefinition
  ): IO[UnexpectedError, CredentialDefinition]

  def issueJwtCredential(
      canonicalPrismDID: CanonicalPrismDID,
      credentialIdentifier: Option[String],
      credentialDefinition: CredentialDefinition
  ): IO[Error, String]
}

object CredentialIssuerService {
  trait Error {
    def message: String
  }

  object Errors {
    case class InvalidProof(message: String) extends Error

    case class UnexpectedError(cause: Throwable) extends Error {
      override def message: String = cause.getMessage
    }
  }
}

case class CredentialIssuerServiceImpl() extends CredentialIssuerService {

  import CredentialIssuerService.Error
  import CredentialIssuerService.Errors.*
  override def verifyJwtProof(jwt: String): IO[InvalidProof, Boolean] = {
    ZIO.succeed(true) // TODO: implement
  }

  override def validateCredentialDefinition(
      credentialDefinition: CredentialDefinition
  ): IO[UnexpectedError, CredentialDefinition] = {
    ZIO.succeed(credentialDefinition) // TODO: implement
  }

  override def issueJwtCredential(
      canonicalPrismDID: CanonicalPrismDID,
      credentialIdentifier: Option[String],
      credentialDefinition: CredentialDefinition
  ): IO[CredentialIssuerService.Error, String] = {
    ZIO.succeed("jwt") // TODO: implement
  }
}

object CredentialIssuerServiceImpl {
  val layer: ULayer[CredentialIssuerService] = ZLayer.succeed(CredentialIssuerServiceImpl())
}
