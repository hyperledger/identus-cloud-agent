package features.multitenancy

import api_models.CreateWalletRequest
import common.Ensure
import common.TestConstants
import common.Utils
import interactions.Get
import interactions.Post
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.*
import java.util.*
import kotlin.random.Random


class WalletsSteps {

    @OptIn(ExperimentalStdlibApi::class)
    fun createNewWallet(
        actor: Actor,
        name: String = "test-wallet",
        seed: String = Random.nextBytes(64).toHexString(),
        id: String = UUID.randomUUID().toString()) {
        actor.attemptsTo(
            Post.to("/wallets")
                .with {
                    it.body(
                        CreateWalletRequest(
                            name = name,
                            seed = seed,
                            id = id,
                        )
                    )
                },
        )
    }

    @When("{actor} creates new wallet with name {string}")
    fun iCreateNewWalletWithName(acme: Actor, name: String) {
        createNewWallet(acme, name)
        acme.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_CREATED)
                .withReportedError("Response status code is not correct!"),
        )
        acme.remember("walletId", Utils.lastResponseObject("id", String::class))
    }

    @When("{actor} creates new wallet with unique id")
    fun acmeCreateNewWalletWithId(acme: Actor) {
        val uniqueId = UUID.randomUUID().toString()
        acme.remember("uniqueId", uniqueId)
        createNewWallet(acme, id = uniqueId)
        acme.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_CREATED)
                .withReportedError("Response status code is not correct!"),
            Ensure.that(Utils.lastResponseObject("id", String::class)).isEqualTo(uniqueId)
                .withReportedError("Wallet id is not correct!"),
        )
    }

    @When("{actor} creates new wallet with the same unique id")
    fun acmeCreateNewWalletWithTheSameId(acme: Actor) {
        createNewWallet(acme, id = acme.recall("uniqueId"))
        acme.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_BAD_REQUEST)
                .withReportedError("Response status code is not correct!"),
        )
    }

    @When("{actor} creates new wallet with unique name")
    fun acmeCreatesNewWalletWithUniqueName(acme: Actor) {
        val name = UUID.randomUUID().toString()
        acme.remember("uniqueName", name)
        createNewWallet(acme, name = name)
        acme.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_CREATED)
                .withReportedError("Response status code is not correct!"),
        )
    }

    @When("{actor} creates new wallet with the same unique name")
    fun acmeCreatesNewWalletWithTheSameUniqueName(acme: Actor) {
        createNewWallet(acme, name = acme.recall("uniqueName"))
        acme.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_CREATED)
                .withReportedError("Response status code is not correct!"),
        )
    }

    @Then("{actor} should have a wallet with name {string}")
    fun iShouldHaveAWalletWithName(acme: Actor, name: String) {
        acme.attemptsTo(
            Get.resource("/wallets/${acme.recall<String>("walletId")}")
                .with {
                    it.queryParam("name", name)
                },
        )
        acme.attemptsTo(
            Ensure.that(Utils.lastResponseObject("name", String::class)).isEqualTo(name)
                .withReportedError("Wallet name is not correct!"),
            Ensure.that(Utils.lastResponseObject("id", String::class)).isEqualTo(acme.recall("walletId"))
                .withReportedError("Wallet id is not correct!"),
        )
    }

    @Then("{actor} should have two wallets with unique name but different ids")
    fun acmeShouldHaveTwoWalletsWithNameButDifferentIds(acme: Actor) {
        acme.attemptsTo(
            Get.resource("/wallets")
        )
        acme.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_OK)
                .withReportedError("Response status code is not correct!"),
        )
        val wallets = Utils.lastResponseList("contents.name", String::class).filter { it == acme.recall("uniqueName") }
        acme.attemptsTo(
            Ensure.that(wallets.size).isEqualTo(2)
                .withReportedError("Two wallets with the same name were not created!"),
        )
    }

    @Then("{actor} should have only one wallet and second operation should fail")
    fun acmeShouldHaveOnlyOneWalletAndSecondOperationShouldFail(acme: Actor) {
        acme.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_BAD_REQUEST)
                .withReportedError("Response status code is not correct!")
        )
        acme.attemptsTo(
            Get.resource("/wallets")
        )
        acme.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_OK)
                .withReportedError("Response status code is not correct!"),
        )
        val wallets = Utils.lastResponseList("contents.id", String::class).filter { it == acme.recall("uniqueId") }
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
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_BAD_REQUEST)
                .withReportedError("Response status code is not correct!"),
        )
    }
}
