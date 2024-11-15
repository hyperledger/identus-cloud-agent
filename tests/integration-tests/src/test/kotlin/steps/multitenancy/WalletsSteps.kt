package steps.multitenancy

import common.TestConstants
import interactions.Get
import interactions.Post
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.*
import org.hyperledger.identus.client.models.CreateWalletRequest
import org.hyperledger.identus.client.models.WalletDetail
import org.hyperledger.identus.client.models.WalletDetailPage
import java.util.*
import kotlin.random.Random

class WalletsSteps {

    @OptIn(ExperimentalStdlibApi::class)
    fun createNewWallet(
        actor: Actor,
        name: String = "test-wallet",
        seed: String = Random.nextBytes(64).toHexString(),
        id: UUID = UUID.randomUUID(),
    ): WalletDetail {
        actor.attemptsTo(
            Post.to("/wallets")
                .with {
                    it.body(
                        CreateWalletRequest(
                            name = name,
                            seed = seed,
                            id = id,
                        ),
                    )
                },
        )
        return SerenityRest.lastResponse().get<WalletDetail>()
    }

    @When("{actor} creates new wallet with name '{}'")
    fun iCreateNewWalletWithName(acme: Actor, name: String) {
        val wallet = createNewWallet(acme, name)
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        acme.remember("walletId", wallet.id)
    }

    @When("{actor} creates new wallet with unique id")
    fun acmeCreateNewWalletWithId(acme: Actor) {
        val uniqueId = UUID.randomUUID()
        acme.remember("uniqueId", uniqueId)
        val wallet = createNewWallet(acme, id = uniqueId)
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
            Ensure.that(wallet.id).isEqualTo(uniqueId)
                .withReportedError("Wallet id is not correct!"),
        )
    }

    @When("{actor} creates new wallet with the same unique id")
    fun acmeCreateNewWalletWithTheSameId(acme: Actor) {
        createNewWallet(acme, id = acme.recall("uniqueId"))
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_UNPROCESSABLE_ENTITY),
        )
    }

    @When("{actor} creates new wallet with unique name")
    fun acmeCreatesNewWalletWithUniqueName(acme: Actor) {
        val name = UUID.randomUUID().toString()
        acme.remember("uniqueName", name)
        createNewWallet(acme, name = name)
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
    }

    @When("{actor} creates new wallet with the same unique name")
    fun acmeCreatesNewWalletWithTheSameUniqueName(acme: Actor) {
        createNewWallet(acme, name = acme.recall("uniqueName"))
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
    }

    @Then("{actor} should have a wallet with name '{}'")
    fun iShouldHaveAWalletWithName(acme: Actor, name: String) {
        acme.attemptsTo(
            Get.resource("/wallets/${acme.recall<String>("walletId")}")
                .with {
                    it.queryParam("name", name)
                },
        )
        val wallet = SerenityRest.lastResponse().get<WalletDetail>()

        acme.attemptsTo(
            Ensure.that(wallet.name).isEqualTo(name)
                .withReportedError("Wallet name is not correct!"),
            Ensure.that(wallet.id).isEqualTo(acme.recall("walletId"))
                .withReportedError("Wallet id is not correct!"),
        )
    }

    @Then("{actor} should have two wallets with unique name but different ids")
    fun acmeShouldHaveTwoWalletsWithNameButDifferentIds(acme: Actor) {
        acme.attemptsTo(
            Get.resource("/wallets"),
        )
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
        val wallets = SerenityRest.lastResponse().get<WalletDetailPage>().contents!!.filter { it.name == acme.recall("uniqueName") }
        acme.attemptsTo(
            Ensure.that(wallets.size).isEqualTo(2)
                .withReportedError("Two wallets with the same name were not created!"),
        )
    }

    @Then("{actor} should have only one wallet and second operation should fail")
    fun acmeShouldHaveOnlyOneWalletAndSecondOperationShouldFail(acme: Actor) {
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_UNPROCESSABLE_ENTITY),
        )
        acme.attemptsTo(
            Get.resource("/wallets"),
        )
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
        val wallets = SerenityRest.lastResponse().get<WalletDetailPage>().contents!!.filter { it.id == acme.recall("uniqueId") }
        acme.attemptsTo(
            Ensure.that(wallets.size).isEqualTo(1)
                .withReportedError("Only one wallet should be created with the same id!"),
        )
    }

    @Given("{actor} creates new wallet with wrong seed")
    fun acmeCreatesNewWalletWithWrongSeed(acme: Actor) {
        createNewWallet(acme, seed = TestConstants.WRONG_SEED)
    }

    @Then("{actor} should see the error and wallet should not be created")
    fun acmeShouldSeeTheErrorAndWalletShouldNotBeCreated(acme: Actor) {
        acme.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_BAD_REQUEST),
        )
    }
}
