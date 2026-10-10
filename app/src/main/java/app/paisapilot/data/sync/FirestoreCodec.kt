package app.paisapilot.data.sync

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * Converts @Serializable rows to and from the flat maps Firestore stores.
 * The local-only [DIRTY] flag never goes to the cloud; rows read from it are clean.
 */
object FirestoreCodec {
    private const val DIRTY = "dirty"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun <T> toMap(serializer: KSerializer<T>, row: T): Map<String, Any?> =
        json.encodeToJsonElement(serializer, row).jsonObject
            .filterKeys { it != DIRTY }
            .mapValues { (_, value) -> value.toPlain() }

    /** Null when the document cannot be read as [T], e.g. written by a newer app version. */
    fun <T> fromMap(serializer: KSerializer<T>, data: Map<String, Any?>): T? {
        val fields = HashMap<String, JsonElement>()
        for ((key, value) in data) {
            fields[key] = when (value) {
                null -> JsonNull
                is Boolean -> JsonPrimitive(value)
                is Long, is Int -> JsonPrimitive((value as Number).toLong())
                is Number -> JsonPrimitive(value.toDouble())
                is String -> JsonPrimitive(value)
                else -> continue // server timestamps and other non-row fields
            }
        }
        fields[DIRTY] = JsonPrimitive(false)
        return runCatching { json.decodeFromJsonElement(serializer, JsonObject(fields)) }.getOrNull()
    }

    private fun JsonElement.toPlain(): Any? = when {
        this is JsonNull -> null
        this is JsonPrimitive && isString -> content
        this is JsonPrimitive -> booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
        else -> toString()
    }
}
