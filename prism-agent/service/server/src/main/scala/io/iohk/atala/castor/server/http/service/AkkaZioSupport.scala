package io.iohk.atala.castor.server.http.service

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.*
import zio.*

trait AkkaZioSupport {

  protected def onZioSuccess[A](z: UIO[A])(f: A => Route)(using runtime: Runtime[Any]): Route = {
    Unsafe.unsafe { implicit unsafe =>
      onSuccess(runtime.unsafe.runToFuture(z))(f)
    }
  }

}
