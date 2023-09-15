package common

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import net.serenitybdd.rest.SerenityRest
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.withPollInterval
import org.awaitility.pollinterval.FixedPollInterval
import java.time.Duration
import kotlin.reflect.KClass

object Utils {

    fun <T : Any> lastResponseObject(path: String, clazz: KClass<T>): T {
        return SerenityRest.lastResponse().jsonPath().getObject(path, clazz.java)
    }

    fun <T : Any> lastResponseList(path: String, clazz: KClass<T>): List<T> {
        return SerenityRest.lastResponse().jsonPath().getList(path, clazz.java)
    }

    fun <T : Any, V: Any> lastResponseMap(path: String, keyType: KClass<T>, valueType: KClass<V>): Map<T, V> {
        return SerenityRest.lastResponse().jsonPath().getMap(path, keyType.java, valueType.java)
    }

    fun toJsonPath(any: Any): DocumentContext {
        val json = ObjectMapper().writeValueAsString(any)
        return JsonPath.parse(json)
    }

    fun wait(
        blockToWait: () -> Boolean,
        errorMessage: String,
        poolInterval: FixedPollInterval = FixedPollInterval(Duration.ofMillis(500L)),
        timeout: Duration = Duration.ofSeconds(120L),
    ) {
        try {
            Awaitility.await().withPollInterval(poolInterval)
                .pollInSameThread()
                .atMost(timeout)
                .until {
                    blockToWait()
                }
        } catch (err: ConditionTimeoutException) {
            throw ConditionTimeoutException(
                errorMessage,
            )
        }
    }
}
