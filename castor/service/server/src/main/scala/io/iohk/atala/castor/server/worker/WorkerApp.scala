package io.iohk.atala.castor.server.worker

import io.iohk.atala.castor.core.model.IrisNotification
import zio.*
import zio.stream.ZStream

object WorkerApp {

  def start: URIO[ZStream[Any, Nothing, IrisNotification] & EventConsumer, Unit] = {
    for {
      source <- ZIO.service[ZStream[Any, Nothing, IrisNotification]]
      consumer <- ZIO.service[EventConsumer]
      _ <- consumer.consumeIrisNotification(source)
    } yield ()
  }

}
