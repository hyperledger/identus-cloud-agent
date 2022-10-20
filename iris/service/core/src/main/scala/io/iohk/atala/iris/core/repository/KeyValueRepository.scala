package io.iohk.atala.iris.core.repository

trait ROKeyValueRepository[F[_]] {
  def get(key: String): F[Option[String]]
  def getInt(key: String): F[Option[Int]]
}

trait KeyValueRepository[F[_]] extends ROKeyValueRepository[F] {
  def set(key: String, value: Option[Int | String]): F[Unit]
}
