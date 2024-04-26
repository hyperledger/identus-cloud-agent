package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.service.URIDereferencerError.ResourceNotFound
import zio.*

import java.net.URI

class ResourceURIDereferencerImpl(extraResources: Map[String, String]) extends URIDereferencer {

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
          extraResources
            .get(uri.toString)
            .map(ZIO.succeed(_))
            .getOrElse(ZIO.fail(ResourceNotFound(uri)))
    } yield body
  }

}

object ResourceURIDereferencerImpl {
  def layer: ULayer[ResourceURIDereferencerImpl] =
    ZLayer.succeed(new ResourceURIDereferencerImpl(Map.empty))

  def layerWithExtraResources: URLayer[Map[String, String], ResourceURIDereferencerImpl] =
    ZLayer.fromFunction(ResourceURIDereferencerImpl(_))
}
