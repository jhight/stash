package com.jhight.stash

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.jhight.stash.crypto.Aes256AndroidKeystoreCryptoProvider
import com.jhight.stash.crypto.CryptoProvider
import com.jhight.stash.serializer.StashSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class Stash(
    private val file: File,
    cryptoProvider: CryptoProvider = Aes256AndroidKeystoreCryptoProvider(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : DataStore<JsonElement> {
    val store = DataStoreFactory.create(
        serializer = StashSerializer(cryptoProvider),
        produceFile = { file },
        scope = scope,
    )

    override val data: Flow<JsonElement>
        get() {
            return store.data
        }

    override suspend fun updateData(transform: suspend (t: JsonElement) -> JsonElement): JsonElement {
        return store.updateData {
            transform(it)
        }
    }

    suspend inline fun <reified T> write(data: T) {
        if (!T::class.java.isAnnotationPresent(Serializable::class.java)) {
            throw IllegalArgumentException("Must be @Serializable")
        }

        updateData {
            data.serialize()
        }
    }

    suspend fun write(key: String, value: JsonElement) {
        updateData {
            it.jsonObject.toMutableMap().apply {
                put(key, value)
            }.let {
                Json.encodeToJsonElement(it)
            }
        }
    }

    suspend inline fun <reified T> write(key: String, value: T) = write(key, value.serialize())

    suspend inline fun <reified T> read(crossinline handler: (T) -> Unit) =
        withContext(Dispatchers.IO) {
            if (!T::class.java.isAnnotationPresent(Serializable::class.java)) {
                throw IllegalArgumentException("Must be @Serializable")
            }

            store.data.firstOrNull()?.deserialize<T>()?.let {
                handler(it)
            }
        }

    suspend fun getJsonElement(key: String): JsonElement? {
        return store.data.firstOrNull()?.jsonObject?.get(key)
    }

    suspend fun getString(key: String) =
        if (getJsonElement(key)?.jsonPrimitive?.isString == true) {
            getJsonElement(key)?.jsonPrimitive?.contentOrNull
        } else {
            null
        }

    suspend fun getInt(key: String) = getJsonElement(key)?.jsonPrimitive?.intOrNull
    suspend fun getDouble(key: String) = getJsonElement(key)?.jsonPrimitive?.doubleOrNull
    suspend fun getFloat(key: String) = getJsonElement(key)?.jsonPrimitive?.floatOrNull
    suspend fun getBoolean(key: String) = getJsonElement(key)?.jsonPrimitive?.booleanOrNull
    suspend inline fun <reified T> get(key: String) = getJsonElement(key)?.let { Json.decodeFromJsonElement<T>(it) }

    suspend inline fun <reified T> edit(crossinline transform: (T) -> Unit) {
        updateData {
            it.deserialize<T?>()?.apply {
                transform(this)
            }.serialize()
        }
    }

    fun close() {
        scope.cancel()
    }
}

val json = Json {
    ignoreUnknownKeys = true
}

inline fun <reified T> JsonElement.deserialize(): T? {
    json.let {
        return it.decodeFromJsonElement(this)
    }
}

inline fun <reified T> T.serialize(): JsonElement {
    json.let {
        return it.encodeToJsonElement(this)
    }
}

fun JsonElement.string(key: String) =
    if (this.jsonObject[key]?.jsonPrimitive?.isString == true) {
        this.jsonObject[key]?.jsonPrimitive?.contentOrNull
    } else {
        null
    }

fun JsonElement.json(key: String) = this.jsonObject[key]
fun JsonElement.int(key: String) = this.jsonObject[key]?.jsonPrimitive?.intOrNull
fun JsonElement.double(key: String) = this.jsonObject[key]?.jsonPrimitive?.doubleOrNull
fun JsonElement.float(key: String) = this.jsonObject[key]?.jsonPrimitive?.floatOrNull
fun JsonElement.boolean(key: String) = this.jsonObject[key]?.jsonPrimitive?.booleanOrNull
fun JsonElement.stringOrNull(key: String) = this.jsonObject[key]?.jsonPrimitive?.contentOrNull

fun JsonObject.edit(transform: (MutableMap<String, JsonElement>) -> Unit): JsonObject =
    toMutableMap().let {
        transform(it)
        JsonObject(it)
    }