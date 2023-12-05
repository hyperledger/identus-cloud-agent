package io.iohk.atala.pollux.sql.repository

import cats.data.NonEmptyList
import doobie.*
import doobie.free.connection
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import io.iohk.atala.castor.core.model.did.*
import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, OfferCredential, RequestCredential}
import io.iohk.atala.pollux.anoncreds.CredentialRequestMetadata
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.error.CredentialRepositoryError
import io.iohk.atala.pollux.core.model.error.CredentialRepositoryError.*
import io.iohk.atala.pollux.core.repository.{CredentialRepository, CredentialStatusListRepository}
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.*
import io.iohk.atala.shared.models.WalletAccessContext
import org.postgresql.util.PSQLException
import zio.*
import zio.interop.catz.*
import zio.json.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}

import java.time.Instant

class JdbcCredentialStatusListRepository(xa: Transactor[ContextAwareTask], xb: Transactor[Task])
    extends CredentialStatusListRepository {
  def getLatestOfTheWallet(walletId: WalletId): RIO[WalletAccessContext, CredentialStatusList] = ???

}

object JdbcCredentialStatusListRepository {
  val layer: URLayer[Transactor[ContextAwareTask] & Transactor[Task], CredentialStatusListRepository] =
    ZLayer.fromFunction(new JdbcCredentialStatusListRepository(_, _))
}
