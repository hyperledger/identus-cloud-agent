package io.iohk.atala.agent.walletapi.util

object SeqExtensions {

  extension [A](s: Seq[A]) {

    // TODO: add test
    /** Selects all the elements of this $coll ignoring the duplicates as determined by `==` after applying the
      * transforming function `f`.
      *
      * @param f
      *   The transforming function whose result is used to determine the uniqueness of each element
      * @param keepFirst
      *   Keeping first element when dropping duplicated elements. Otherwise, keep last.
      * @tparam B
      *   the type of the elements after being transformed by `f`
      * @return
      *   a new $coll consisting of all the elements of this $coll without duplicates.
      */
    def distinctBy[B](f: A => B, keepFirst: Boolean): Seq[A] = {
      val sorted = if (keepFirst) s else s.reverse
      val (result, _) = sorted.foldLeft((List.empty[A], Set.empty[B])) { case ((acc, keySeen), i) =>
        val key = f(i)
        if (keySeen.contains(key)) (acc, keySeen)
        else (i :: acc, keySeen + key)
      }
      if (keepFirst) result.reverse else result
    }

  }

}
