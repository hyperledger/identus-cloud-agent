package io.iohk.atala.agent.walletapi.util

import zio.*
import zio.test.*
import zio.test.Assertion.*
import SeqExtensions.*

object SeqExtensionsSpec extends ZIOSpecDefault {

  override def spec = suite("SeqExtensions")(distinctBySpec)

  private val distinctBySpec = {
    final case class KV(key: String, value: Int)

    suite("distinctBy")(
      test("return empty sequence for empty sequence") {
        val ls: List[KV] = Nil
        assert(ls)(equalTo(ls.distinctBy(_.key, keepFirst = true))) &&
        assert(ls)(equalTo(ls.distinctBy(_.key, keepFirst = false)))
      },
      test("return same sequence if no duplication found") {
        val ls = (1 to 10).map(i => KV(i.toString, i))
        assert(ls)(equalTo(ls.distinctBy(_.key, keepFirst = true))) &&
        assert(ls)(equalTo(ls.distinctBy(_.key, keepFirst = false)))
      },
      test("return first of duplicated elements found") {
        val ls = List(
          KV("1", 1),
          KV("2", 2),
          KV("1", 3),
          KV("1", 4),
          KV("2", 5),
          KV("3", 6)
        )
        assert(ls.distinctBy(_.key, keepFirst = true))(
          equalTo(
            List(
              KV("1", 1),
              KV("2", 2),
              KV("3", 6)
            )
          )
        )
      },
      test("return last of duplicated elements found") {
        val ls = List(
          KV("1", 1),
          KV("2", 2),
          KV("1", 3),
          KV("1", 4),
          KV("2", 5),
          KV("3", 6)
        )
        assert(ls.distinctBy(_.key, keepFirst = false))(
          equalTo(
            List(
              KV("1", 4),
              KV("2", 5),
              KV("3", 6)
            )
          )
        )
      }
    )
  }

}
