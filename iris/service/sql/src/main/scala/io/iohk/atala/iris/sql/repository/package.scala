package io.iohk.atala.iris.sql

import doobie.*
import fs2.Stream
import zio.stream as zstream

package object repository {
  type IO[A] = ConnectionIO[A]
  type StreamIO[A] = Stream[ConnectionIO, A]

  type StreamZIO[A] = zstream.Stream[Throwable, A]
}
