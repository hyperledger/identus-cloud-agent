package io.iohk.atala.multitenancy.core.repository

import io.iohk.atala.multitenancy.core.repository.DidWalletMappingRepositorySpecSuite.testSuite
import zio.*
import zio.test.*

object DidWalletMappingServiceInMemorySpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("In Memory DidWalletMapping Repository test suite")(testSuite).provide(
      DidWalletMappingRepositoryInMemory.layer
    )

}
