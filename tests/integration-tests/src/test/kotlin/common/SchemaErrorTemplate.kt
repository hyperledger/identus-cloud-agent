package common

import com.google.gson.Gson
import com.google.gson.JsonObject
import common.CredentialSchema.STUDENT_SCHEMA
import net.serenitybdd.screenplay.Actor

enum class SchemaErrorTemplate {
    TYPE_AND_PROPERTIES_WITHOUT_SCHEMA_TYPE {
        override fun inner_schema(): String {
            return """
                {
                    "type": "object",
                    "properties": {
                        "name": {
                            "type": "string"
                        },
                        "age": {
                            "type": "integer"
                        }
                    },
                    "required": ["name"]
                }
            """.trimIndent()
        }
    },
    CUSTOM_WORDS_NOT_DEFINED {
        override fun inner_schema(): String {
            return """
                {
                  "${"$"}schema": "http://json-schema.org/draft-2020-12/schema#",
                  "type": "object",
                  "properties": {
                    "name": {
                      "type": "string"
                    },
                    "age": {
                      "type": "integer"
                    }
                  },
                  "customKeyword": "value"
                }
            """.trimIndent()
        }
    },
    MISSING_REQUIRED_FOR_MANDATORY_PROPERTY {
        override fun inner_schema(): String {
            return """
            {
              "${"$"}schema": "http://json-schema.org/draft-2020-12/schema#",
              "type": "object",
              "properties": {
                "name": {
                  "type": "string"
                },
                "age": {
                  "type": "integer"
                }
              }
            }
            """
        }
    }, ;

    abstract fun inner_schema(): String

    fun schema(actor: Actor): String {
        val innerSchema = Gson().fromJson(inner_schema(), JsonObject::class.java)
        val json = getJson(actor)
        json.add("schema", innerSchema)
        return json.toString()
    }

    private fun getJson(actor: Actor): JsonObject {
        val jsonString = Gson().toJson(STUDENT_SCHEMA.credentialSchema.copy(author = actor.recall("shortFormDid")))
        return Gson().fromJson(jsonString, JsonObject::class.java)
    }
}
