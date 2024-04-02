package com.jhight.stash

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jhight.stash.crypto.Aes256AndroidKeystoreCryptoProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreAndroidTest {
    @Test
    fun testWritesAndReads(): Unit = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.dataDir, "readsAndWrites.stash")
        var stash = Stash(file)

        @Serializable
        data class Data2(
            val x: Int?,
            val y: Float?,
            val z: Double?,
        )

        @Serializable
        data class Data1(
            val a: String,
            val b: Double,
            val c: Int,
            val d: Boolean,
            val e: String?,
            val f: List<String>,
            val g: Map<String, Int>,
            val h: Data2,
        )

        val data1 = Data1(
            a = "Hello, world!",
            b = 120.94,
            c = 3,
            d = true,
            e = null,
            f = listOf("a", "b", "c"),
            g = mapOf("x" to 1, "y" to 2, "z" to 3),
            h = Data2(null, 2.7f, 3.25)
        )

        stash.write(data1)

        stash.read<Data1> {
            assertEquals("Hello, world!", it.a)
            assertEquals(120.94, it.b)
            assertEquals(3, it.c)
            assertEquals(true, it.d)
            assertNull(it.e)
            assertEquals(listOf("a", "b", "c"), it.f)
            assertEquals(mapOf("x" to 1, "y" to 2, "z" to 3), it.g)
            assertEquals(null, it.h.x)
        }

        stash.getJsonElement("h")?.jsonObject?.get("y")?.let {
            assertEquals(2.7f, it.jsonPrimitive.float)
        }

        stash.getJsonElement("h")?.jsonObject?.get("z")?.let {
            assertEquals(3.25, it.jsonPrimitive.double)
        }

        stash.close()

        // reopen stash
        stash = Stash(file)

        stash.getJsonElement("a")?.jsonPrimitive?.let {
            assertEquals("Hello, world!", it.content)
        }
    }

    @Test
    fun testEncryption(): Unit = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.dataDir, "${System.currentTimeMillis()}.stash")
        val cryptoProvider = Aes256AndroidKeystoreCryptoProvider()
        val stash = Stash(file, cryptoProvider)

        @Serializable
        data class Data(
            val secret: String
        )

        stash.write(Data("This is a secret message."))

        assertTrue(file.exists())
        assertTrue(file.canRead())

        val encryptedData = file.readText()
        assertNotNull(encryptedData)
        Assert.assertNotEquals("""{"secret":"This is a secret message."}""", encryptedData)

        val plaintext = cryptoProvider.decrypt(file.readBytes()).toString(Charsets.UTF_8)
        assertEquals("""{"secret":"This is a secret message."}""", plaintext)
    }
}