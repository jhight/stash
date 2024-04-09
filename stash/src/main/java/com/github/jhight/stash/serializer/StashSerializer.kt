package com.github.jhight.stash.serializer

import androidx.datastore.core.Serializer
import androidx.datastore.preferences.protobuf.CodedOutputStream
import com.github.jhight.stash.crypto.CryptoProvider
import com.github.jhight.stash.serialize
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

internal class StashSerializer (
    private val cryptoProvider: CryptoProvider,
    override val defaultValue: JsonElement = JsonObject(emptyMap()),
) : Serializer<JsonElement> {
    override suspend fun readFrom(input: InputStream): JsonElement {
        val encrypted = input.readBytes()
        val decrypted = if (encrypted.isNotEmpty()) {
            cryptoProvider.decrypt(encrypted)
        } else {
            ByteArray(0)
        }
        return Json.parseToJsonElement(decrypted.toString(Charsets.UTF_8))
    }

    override suspend fun writeTo(t: JsonElement, output: OutputStream) {
        val plaintext = Json.encodeToString(t).toByteArray(Charsets.UTF_8)
        val encrypted = cryptoProvider.encrypt(plaintext)
        val codedOutput = CodedOutputStream.newInstance(output)
        codedOutput.write(ByteBuffer.wrap(encrypted))
        codedOutput.flush()
    }
}


