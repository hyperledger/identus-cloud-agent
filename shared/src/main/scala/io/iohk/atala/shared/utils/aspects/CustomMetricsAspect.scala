package io.iohk.atala.shared.utils.aspects

import zio.*
import scala.collection.mutable.{Map => MutMap}
import zio.metrics.*

object CustomMetricsAspect {
  private val checkpoints: MutMap[String, Long] = MutMap.empty
  private def currTime = ZIO.succeed(java.lang.System.nanoTime)

  def recordTimeAfter(key: String): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      override def apply[R, E, A](
          zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] =
        for {
          res <- zio
          timeAfter <- currTime
          _ = checkpoints.update(key, timeAfter)
        } yield res
    }

  def createGaugeAfter(key: String, metricsKey: String, tags: Set[MetricLabel] = Set.empty): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      override def apply[R, E, A](
          zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] = {
        for {
          res <- zio
          timeAfter <- currTime
          timeBefore = checkpoints.get(key)
          metricsZio = timeBefore.map(before => timeAfter - before) match {
            case Some(value) =>
              ZIO.succeed(value.toDouble) @@ Metric.gauge(metricsKey).tagged(tags)
            case None => ZIO.unit
          }
          _ <- metricsZio
        } yield res
      }
    }

}
