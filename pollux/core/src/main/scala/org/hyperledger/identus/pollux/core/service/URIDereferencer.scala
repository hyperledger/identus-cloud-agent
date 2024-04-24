package org.hyperledger.identus.pollux.core.service

import zio.IO

import java.net.URI

trait URIDereferencer {
  def dereference(uri: URI): IO[URIDereferencerError, String]
}

sealed trait URIDereferencerError {
  def error: String
}

object URIDereferencerError {
  final case class ConnectionError(error: String) extends URIDereferencerError
  final case class ResourceNotFound(uri: URI) extends URIDereferencerError:
    override def error: String = uri.toString
  final case class UnexpectedError(error: String) extends URIDereferencerError
}
