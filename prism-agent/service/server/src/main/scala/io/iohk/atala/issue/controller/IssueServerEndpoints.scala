package io.iohk.atala.issue.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.connect.controller.ConnectionEndpoints.*
import io.iohk.atala.connect.controller.http.CreateConnectionRequest
import io.iohk.atala.pollux.credentialschema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController
import sttp.tapir.ztapir.*
import zio.{URIO, ZIO}
