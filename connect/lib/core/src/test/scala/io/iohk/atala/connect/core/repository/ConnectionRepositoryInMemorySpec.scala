package io.iohk.atala.connect.core.repository

import zio._
import zio.test._

object ConnectionRepositoryInMemorySpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("In Memory Connection Repository test suite")(ConnectionRepositorySpecSuite.testSuite).provide(
      ConnectionRepositoryInMemory.layer
    )

  // TODO: not good enough I want to test the SQL queries!

}
