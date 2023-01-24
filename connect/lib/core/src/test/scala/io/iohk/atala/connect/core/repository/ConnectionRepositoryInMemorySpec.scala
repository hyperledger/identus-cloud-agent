package io.iohk.atala.connect.core.repository

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord._
import io.iohk.atala.connect.core.repository.ConnectionRepository
import io.iohk.atala.connect.core.repository.ConnectionRepositoryInMemory
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import zio._
import zio.test.Assertion._
import zio.test._

import java.sql.SQLException
import java.time.Instant
import java.util.UUID

object ConnectionRepositoryInMemorySpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("In Memory Connection Repository test suite")(ConnectionRepositorySpecSuite.testSuite).provide(
      ConnectionRepositoryInMemory.layer
    )

}
