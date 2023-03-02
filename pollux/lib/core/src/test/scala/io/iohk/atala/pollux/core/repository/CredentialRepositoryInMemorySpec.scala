package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.IssueCredentialRecord._
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.core.repository.CredentialRepositoryInMemory
import zio._
import zio.test.Assertion._
import zio.test._

import java.sql.SQLException
import java.time.Instant
import java.util.UUID

/** core/testOnly io.iohk.atala.pollux.core.repository.CredentialRepositoryInMemorySpec */
object CredentialRepositoryInMemorySpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("Credential Repository In Memory test suite")(CredentialRepositorySpecSuite.testSuite).provide(
      CredentialRepositoryInMemory.layer
    )

}
