package io.iohk.atala.shared.db

case class DbConfig(
    username: String,
    password: String,
    jdbcUrl: String,
    awaitConnectionThreads: Int = 8
)
