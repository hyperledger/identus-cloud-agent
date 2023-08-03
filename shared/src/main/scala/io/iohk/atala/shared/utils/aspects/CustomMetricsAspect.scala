package io.iohk.atala.shared.utils.aspects
import java.util.concurrent.TimeUnit
import zio.*
import zio.metrics.*

object CustomMetricsAspect {

  def attachDurationGaugeMetric(name: String): ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, Nothing, Any] {
      override def apply[R, E, A](
          zio: ZIO[R, E, A]
      )(implicit trace: Trace): ZIO[R, E, A] =
        def currTime = Clock.currentTime(TimeUnit.MILLISECONDS)

        for {
          timeBefore <- currTime
          res <- zio
          timeAfter <- currTime
          _ <- ZIO.succeed((timeAfter - timeBefore).toDouble) @@ Metric.gauge(name)
        } yield res
    }
}
