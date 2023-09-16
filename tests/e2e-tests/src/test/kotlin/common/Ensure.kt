package common

import net.serenitybdd.screenplay.Question
import org.openqa.selenium.By
import java.time.LocalDate
import java.time.LocalTime
import net.serenitybdd.screenplay.ensure.enableSoftAssertions as EnableSoftAssertions
import net.serenitybdd.screenplay.ensure.reportSoftAssertions as ReportSoftAssertions
import net.serenitybdd.screenplay.ensure.that as That
import net.serenitybdd.screenplay.ensure.thatAmongst as ThatAmongst
import net.serenitybdd.screenplay.ensure.thatTheCurrentPage as ThatTheCurrentPage
import net.serenitybdd.screenplay.ensure.thatTheListOf as ThatTheListOf
import net.serenitybdd.screenplay.targets.Target as SerenityTarget

object Ensure {
    fun that(value: String?) = That(value)
    fun that(value: LocalDate) = That(value)
    fun that(value: LocalTime) = That(value)
    fun that(value: Boolean) = That(value)
    fun that(value: Float) = That(value)
    fun that(value: Double) = That(value)

    fun <A> that(value: Comparable<A>) = That(value)
    fun <A> that(value: Collection<A>) = That(value)

    fun <A> that(question: Question<A>, predicate: (actual: A) -> Boolean) = That(question, predicate)
    fun <A> that(description: String, question: Question<A>, predicate: (actual: A) -> Boolean) =
        That(description, question, predicate)

    fun <A : Comparable<A>> that(description: String, question: Question<A>) = That(description, question)
    fun <A : Comparable<A>> that(question: Question<A>) = That(question)

    fun <A> that(description: String, question: Question<Collection<A>>) = That(description, question)
    fun <A> that(question: Question<Collection<A>>) = That(question)

    fun <A> thatTheListOf(description: String, question: Question<List<A>>) = ThatTheListOf(description, question)
    fun <A> thatTheListOf(question: Question<List<A>>) = ThatTheListOf(question)

    fun thatTheCurrentPage() = ThatTheCurrentPage()
    fun that(value: SerenityTarget) = That(value)
    fun that(value: By) = net.serenitybdd.screenplay.ensure.that(value)

    // Collection matchers
    fun thatTheListOf(value: SerenityTarget) = ThatTheListOf(value)
    fun thatTheListOf(value: By) = ThatTheListOf(value)

    fun thatAmongst(value: SerenityTarget) = ThatAmongst(value)
    fun thatAmongst(value: By) = ThatAmongst(value)

    fun enableSoftAssertions() = EnableSoftAssertions()
    fun reportSoftAssertions() = ReportSoftAssertions()
}
