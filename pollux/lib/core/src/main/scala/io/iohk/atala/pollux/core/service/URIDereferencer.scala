package io.iohk.atala.pollux.core.service

import zio.{IO, Task}

import java.net.URI

trait URIDereferencer {
  def dereference(uri: URI): IO[URIDereferencerError, String]
}

sealed trait URIDereferencerError

object URIDereferencerError {
  final case class ConnectionError(error: String) extends URIDereferencerError
  final case class ResourceNotFound(uri: URI) extends URIDereferencerError
  final case class UnexpectedError(error: String) extends URIDereferencerError
}
