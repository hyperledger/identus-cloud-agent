package org.hyperledger.identus.pollux.core.repository

import zio._
import zio.test._

object PresentationRepositoryInMemorySpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("Presentation Repository In Memory test suite")(
      PresentationRepositorySpecSuite.testSuite,
      PresentationRepositorySpecSuite.multitenantTestSuite
    ).provide(PresentationRepositoryInMemory.layer)

}
