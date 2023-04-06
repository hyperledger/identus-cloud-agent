package io.iohk.atala.pollux.core.service

import zio.Task

import java.net.URI

trait URIDereferencer {
  def dereference(uri: URI): Task[String]
}
