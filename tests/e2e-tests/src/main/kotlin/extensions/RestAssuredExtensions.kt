package extensions

import io.restassured.response.Response
import java.util.LinkedList
import kotlin.reflect.KClass

fun <T : Any> Response.getRootObject(typeRef: KClass<T>): T {
    return this.jsonPath().getObject("", typeRef.java)
}

fun <T: Any> Response.getObject(path: String, typeRef: KClass<T>): T {
    return this.jsonPath().getObject(path, typeRef.java)
}

fun <T : Any> Response.getRootList(typeRef: KClass<T>): List<T> {
    return this.jsonPath().getList("", typeRef.java)
}
