package com.jhight.stash

import com.jhight.stash.crypto.SimpleCryptoProvider
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.Test
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoTest {
    @Test
    fun `test encrypt and decrypt`() = runTest {
        val cryptoProvider = SimpleCryptoProvider()

        val plaintext = "This is a secret message."
        val ciphertext = cryptoProvider.encrypt(plaintext.toByteArray(Charsets.UTF_8))
        val decrypted = cryptoProvider.decrypt(ciphertext).toString(Charsets.UTF_8)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `test manual encrypt and crypto provider decrypt`() = runTest {
        val key = SecretKeySpec(SecureRandom().generateSeed(16), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = IvParameterSpec(SecureRandom().generateSeed(16))

        val cryptoProvider = SimpleCryptoProvider(key, cipher, ivSpec)
        val plaintext = "This is a secret message."

        // manually encrypt message
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // decrypt using crypto provider
        val decrypted = cryptoProvider.decrypt(cipherText).toString(Charsets.UTF_8)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `test crypto provider encrypt and manual decrypt`() = runTest {
        val key = SecretKeySpec(SecureRandom().generateSeed(16), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = IvParameterSpec(SecureRandom().generateSeed(16))

        val cryptoProvider = SimpleCryptoProvider(key, cipher, ivSpec)
        val plaintext = "This is a secret message."

        // encrypt using crypto provider
        val encrypted = cryptoProvider.encrypt(plaintext.toByteArray(Charsets.UTF_8))

        // manually decrypt message
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val decrypted = cipher.doFinal(encrypted).toString(Charsets.UTF_8)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `test attempt to read with different crypto provider fails`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        var stash = Stash(file, SimpleCryptoProvider())

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

        stash = Stash(file, SimpleCryptoProvider())

        try {
            stash.get<Data> {
                TestCase.fail()
            }
        } catch (e: Throwable) {
            // success
        }
    }
}