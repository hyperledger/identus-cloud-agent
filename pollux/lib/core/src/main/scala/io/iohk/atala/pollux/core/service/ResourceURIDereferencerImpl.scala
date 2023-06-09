package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.service.URIDereferencerError.{ConnectionError, ResourceNotFound, UnexpectedError}
import zio.http.*
import zio.http.ZClient.ClientLive
import zio.http.model.*
import zio.{IO, Layer, Scope, ULayer, ZIO, ZLayer}

import java.net.URI

class ResourceURIDereferencerImpl extends URIDereferencer {

  override def dereference(uri: URI): IO[URIDereferencerError, String] = {
    for {
      scheme <- ZIO.succeed(uri.getScheme)
      body <- scheme match
        case "resource" =>
          val inputStream = this.getClass.getResourceAsStream(uri.getPath)
          if (inputStream != null)
            val content = scala.io.Source.fromInputStream(inputStream).mkString
            inputStream.close()
            ZIO.succeed(content)
          else ZIO.fail(ResourceNotFound(uri))
        case _ =>
          ZIO.fail(ResourceNotFound(uri))
    } yield body
  }

}

object ResourceURIDereferencerImpl {
  def layer: ULayer[URIDereferencer] = ZLayer.succeed(ResourceURIDereferencerImpl())
}
