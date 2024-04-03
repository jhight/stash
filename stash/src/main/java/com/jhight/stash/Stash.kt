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

/**
 * Stash is a simple, encrypted Android DataStore that makes it easy to securely store small amounts
 * of data via the Android Keystore system.
 *
 * Example usage:
 * ```
 * // Create a Stash instance
 * val stash = Stash(File(context.dataDir, "data.stash"))
 *
 * // declare a Serializable data class
 * @Serializable
 * data class Account(
 *    val userId: String,
 *    val balance: Double
 * )
 *
 * // write the object's contents to the stash
 * stash.put(Account("user-12345", 99.50))
 *
 * // alternatively, you can write individual properties - this is equivalent to the example above
 * stash.put("userId", "user-12345")
 * stash.put("balance", 99.50)
 *
 * // to read the data back, use the read function
 * stash.get<Account> {
 *    println(it.userId)
 *    println(it.balance)
*  }
 *
 * // or, you can read individual properties
 * val userId = stash.get<String>("userId")
 * val balance = stash.get<Double>("balance")
 * ```
 *
 * @param file The file to store the data in.
 * @param cryptoProvider The [CryptoProvider] to use for encryption. By default, this is [Aes256AndroidKeystoreCryptoProvider], using a key with a default alias.
 * @param scope The [CoroutineScope] to use for the [DataStore]. By default, this is [Dispatchers.IO].
 * @see [DataStore]
 */
class Stash(
    private val file: File,
    cryptoProvider: CryptoProvider = Aes256AndroidKeystoreCryptoProvider(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : DataStore<JsonElement> {
    /**
     * The underlying [DataStore].
     */
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

    /**
     * Writes a [Serializable] object to the [Stash].
     * @param data The object to write.
     * @throws IllegalArgumentException if the object is not annotated with @Serializable.
     */
    suspend inline fun <reified T> put(data: T) {
        if (!T::class.java.isAnnotationPresent(Serializable::class.java)) {
            throw IllegalArgumentException("Must be @Serializable")
        }

        updateData {
            data.serialize()
        }
    }

    /**
     * Writes a [JsonElement] to the [Stash].
     * @param key The key to write the value to.
     * @param value The value to write.
     */
    suspend fun put(key: String, value: JsonElement) {
        updateData {
            it.jsonObject.toMutableMap().apply {
                put(key, value)
            }.let {
                Json.encodeToJsonElement(it)
            }
        }
    }

    /**
     * Writes a [Serializable] object to the [Stash] at the provided key.
     * @param key The key to write the value to.
     * @param value The value to write.
     */
    suspend inline fun <reified T> put(key: String, value: T) = put(key, value.serialize())

    /**
     * Reads a [Serializable] object from the [Stash].
     * @param handler The handler to call with the deserialized object.
     */
    suspend inline fun <reified T> get(crossinline handler: (T) -> Unit) =
        withContext(Dispatchers.IO) {
            if (!T::class.java.isAnnotationPresent(Serializable::class.java)) {
                throw IllegalArgumentException("Must be @Serializable")
            }

            store.data.firstOrNull()?.deserialize<T>()?.let {
                handler(it)
            }
        }

    /**
     * Reads a [JsonElement] from the [Stash].
     * @param key The key to read the value from.
     * @return The [JsonElement] at the provided key, if it exists.
     */
    suspend fun getJsonElement(key: String): JsonElement? {
        return store.data.firstOrNull()?.jsonObject?.get(key)
    }

    /**
     * Reads a [Serializable] object from the [Stash] at the provided key.
     */
    suspend inline fun <reified T> get(key: String) = getJsonElement(key)?.let { Json.decodeFromJsonElement<T>(it) }

    /**
     * Edits the [Stash] with the provided [transform] function.
     * @param transform The function to transform the [Serializable].
     * @throws IllegalArgumentException if the object is not annotated with @Serializable.
     */
    suspend inline fun <reified T> edit(crossinline transform: (T) -> Unit) {
        if (!T::class.java.isAnnotationPresent(Serializable::class.java)) {
            throw IllegalArgumentException("Must be @Serializable")
        }

        updateData {
            it.deserialize<T?>()?.apply {
                transform(this)
            }.serialize()
        }
    }

    /**
     * Closes the stash. This is not required, but is needed before opening the same stash more than once.
     */
    fun close() {
        scope.cancel()
    }
}

val json = Json {
    ignoreUnknownKeys = true
}

/**
 * Deserializes a [JsonElement] to a [Serializable] object.
 */
inline fun <reified T> JsonElement.deserialize(): T? {
    json.let {
        return it.decodeFromJsonElement(this)
    }
}

/**
 * Serializes an object into a [JsonElement].
 */
inline fun <reified T> T.serialize(): JsonElement {
    json.let {
        return it.encodeToJsonElement(this)
    }
}

/**
 * Gets the String value of a [JsonElement] at the provided key, if it exists.
 */
fun JsonElement.string(key: String) =
    if (this.jsonObject[key]?.jsonPrimitive?.isString == true) {
        this.jsonObject[key]?.jsonPrimitive?.contentOrNull
    } else {
        null
    }

/**
 * Gets the Int value of a [JsonElement] at the provided key, if it exists.
 */
fun JsonElement.int(key: String) = this.jsonObject[key]?.jsonPrimitive?.intOrNull

/**
 * Gets the Double value of a [JsonElement] at the provided key, if it exists.
 */
fun JsonElement.double(key: String) = this.jsonObject[key]?.jsonPrimitive?.doubleOrNull

/**
 * Gets the Float value of a [JsonElement] at the provided key, if it exists.
 */
fun JsonElement.float(key: String) = this.jsonObject[key]?.jsonPrimitive?.floatOrNull

/**
 * Gets the Boolean value of a [JsonElement] at the provided key, if it exists.
 */
fun JsonElement.boolean(key: String) = this.jsonObject[key]?.jsonPrimitive?.booleanOrNull

/**
 * Edits a [JsonObject] with the provided [transform] function.
 */
fun JsonObject.edit(transform: (MutableMap<String, JsonElement>) -> Unit): JsonObject =
    toMutableMap().let {
        transform(it)
        JsonObject(it)
    }