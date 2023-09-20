package io.iohk.atala.pollux.core.service

import io.iohk.atala.agent.walletapi.memory.GenericSecretStorageInMemory
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.schema.CredentialDefinition
import io.iohk.atala.pollux.core.repository.CredentialDefinitionRepositoryInMemory
import io.iohk.atala.shared.models.WalletId.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*

import java.time.OffsetDateTime

trait CredentialDefinitionServiceSpecHelper {

  protected val defaultWalletLayer = ZLayer.succeed(WalletAccessContext(WalletId.default))

  protected val credentialDefinitionServiceLayer =
    GenericSecretStorageInMemory.layer ++ CredentialDefinitionRepositoryInMemory.layer ++ ResourceURIDereferencerImpl.layer >>>
      CredentialDefinitionServiceImpl.layer ++ defaultWalletLayer

  val defaultDefinition =
    """
      |{
      |  "name": "Anoncred",
      |  "version": "1.0",
      |  "attrNames": ["attr1", "attr2"],
      |  "issuerId": "issuer"
      |}
      |""".stripMargin

  extension (svc: CredentialDefinitionService)
    def createRecord(
        name: String = "Name",
        description: String = "Description",
        version: String = "V1",
        authored: Option[OffsetDateTime],
        tag: String = "Tag1",
        author: String = "did:prism:issuer",
        schemaId: String,
        signatureType: String = "CL",
        supportRevocation: Boolean = false
    ): svc.Result[CredentialDefinition] = {
      svc.create(
        CredentialDefinition.Input(
          name = name,
          description = description,
          version = version,
          authored = authored,
          tag = tag,
          author = author,
          schemaId = schemaId,
          signatureType = signatureType,
          supportRevocation = supportRevocation
        )
      )
    }
}
