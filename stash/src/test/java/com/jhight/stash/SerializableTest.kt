package com.jhight.stash

import com.jhight.stash.crypto.Aes128CryptoProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class SerializableTest {
    @Test
    fun `test write and read`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        @Serializable
        data class Data(
            val a: String,
            val b: Double,
            val c: Int,
            val d: Boolean,
        )

        val data = Data(
            a = "Hello, world!",
            b = 120.94,
            c = 3,
            d = true,
        )

        stash.write(data)

        stash.read<Data> {
            assertEquals("Hello, world!", it.a)
            assertEquals(120.94, it.b)
            assertEquals(3, it.c)
            assertTrue(it.d)
        }
    }

    @Test
    fun `test write and read single property`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        @Serializable
        data class Data(
            val a: String,
            val b: Double,
            val c: Int,
            val d: Boolean,
        )

        val data = Data(
            a = "Hello, world!",
            b = 120.94,
            c = 3,
            d = true,
        )

        stash.write(data)

        assertEquals(120.94, stash.get<Double>("b"))
    }

    @Test
    fun `test write read close and re-read`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        val cryptoProvider = Aes128CryptoProvider()
        var stash = Stash(file, cryptoProvider)

        @Serializable
        data class Data(
            val a: String,
            val b: Double,
            val c: Int,
            val d: Boolean,
        )

        val data = Data(
            a = "Hello, world!",
            b = 120.94,
            c = 3,
            d = true,
        )

        stash.write(data)

        stash.read<Data> {
            assertEquals("Hello, world!", it.a)
            assertEquals(120.94, it.b)
            assertEquals(3, it.c)
            assertTrue(it.d)
        }

        stash.close()

        stash = Stash(file, cryptoProvider)

        stash.read<Data> {
            assertEquals("Hello, world!", it.a)
            assertEquals(120.94, it.b)
            assertEquals(3, it.c)
            assertTrue(it.d)
        }
    }

    @Test
    fun `test write json and read as serializable`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        val cryptoProvider = Aes128CryptoProvider()
        val stash = Stash(file, cryptoProvider)

        val json = Json.parseToJsonElement(
            """
                {
                    "a": "Hello, world!",
                    "b": 120.94,
                    "c": 3,
                    "d": true
                }
            """.trimIndent()
        )

        stash.write(json)

        @Serializable
        data class Data(
            val a: String,
            val b: Double,
            val c: Int,
            val d: Boolean,
        )

        stash.read<Data> {
            assertEquals("Hello, world!", it.a)
            assertEquals(120.94, it.b)
            assertEquals(3, it.c)
            assertTrue(it.d)
        }

        stash.close()
    }

    @Test
    fun `test write edit single property and read`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        @Serializable
        data class Data(
            var a: String,
            val b: Double,
        )

        val data = Data(
            a = "Hello, world!",
            b = 120.94,
        )

        stash.write(data)

        data.a = "Goodbye, world!"

        stash.write(data)

        stash.read<Data> {
            assertFalse(it.a == "Hello, world!")
            assertEquals("Goodbye, world!", it.a)
            assertEquals(120.94, it.b)
        }
    }

    @Test
    fun `test write edit and read`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        @Serializable
        data class Data(
            var a: String,
            var b: Double,
        )

        val data = Data(
            a = "Hello, world!",
            b = 120.94,
        )

        stash.write(data)

        stash.read<Data> {
            assertEquals("Hello, world!", it.a)
        }

        stash.edit<Data> {
            it.a = "Goodbye, world!"
        }

        stash.read<Data> {
            assertFalse(it.a == "Hello, world!")
            assertEquals("Goodbye, world!", it.a)
            assertEquals(120.94, it.b)
        }
    }

    @Test
    fun `test overwrite with different data`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        @Serializable
        data class Data1(
            var a: String,
            var b: Double,
        )

        @Serializable
        data class Data2(
            var c: String,
        )

        val data1 = Data1("Hello, world!", 120.94)
        val data2 = Data2("Goodbye, world!")

        stash.write(data1)

        try {
            stash.write(data2)
        } catch (e: Throwable) {
            fail()
        }

        stash.read<Data2> {
            assertFalse(it.c == "Hello, world!")
            assertEquals("Goodbye, world!", it.c)
        }
    }

    @Test
    fun `test write and read incorrectly typed property fails`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        @Serializable
        data class Data(
            val a: String,
            val b: Double,
            val c: Int,
            val d: Boolean,
        )

        val data = Data(
            a = "Hello, world!",
            b = 120.94,
            c = 3,
            d = true,
        )

        stash.write(data)

        try {
            assertEquals(120.94, stash.get<Int>("b"))
            fail()
        } catch (e: Throwable) {
            // succeed
        }
    }

    @Test
    fun `test attempt to write non-serializable data fails`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        data class Data(
            val a: String,
        )

        try {
            stash.write(Data("Hello, world!"))
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Must be @Serializable", e.message)
        }
    }

    @Test
    fun `test attempt to read non-serializable data fails`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        data class Data(
            val a: String,
        )

        try {
            stash.read<Data> {
                fail()
            }
        } catch (e: IllegalArgumentException) {
            assertEquals("Must be @Serializable", e.message)
        }
    }
}