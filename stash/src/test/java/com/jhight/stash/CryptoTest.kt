package com.jhight.stash

import com.jhight.stash.Stash
import com.jhight.stash.crypto.Aes128CryptoProvider
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Test
import java.io.File

class CryptoTest {
    @Test
    fun `test encrypt and decrypt`() = runTest {
        val cryptoProvider = Aes128CryptoProvider()

        val plaintext = "This is a secret message."
        val ciphertext = cryptoProvider.encrypt(plaintext.toByteArray(Charsets.UTF_8))
        val decrypted = cryptoProvider.decrypt(ciphertext).toString(Charsets.UTF_8)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `test attempt to read with different crypto provider fails`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        var stash = Stash(file, Aes128CryptoProvider())

        @Serializable
        data class Data(
            var a: String,
        )

        val data = Data(
            a = "Hello, world!",
        )

        stash.put(data)

        stash.get<Data> {
            assertEquals("Hello, world!", it.a)
        }

        stash.close()

        stash = Stash(file, Aes128CryptoProvider())

        try {
            stash.get<Data> {
                TestCase.fail()
            }
        } catch (e: Throwable) {
            // success
        }
    }
}