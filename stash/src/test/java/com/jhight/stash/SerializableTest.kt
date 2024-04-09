package com.jhight.stash

import com.jhight.stash.crypto.SimpleCryptoProvider
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
            SimpleCryptoProvider()
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

        stash.put(data)

        stash.get<Data> {
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
            SimpleCryptoProvider()
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

        stash.put(data)

        assertEquals(120.94, stash.get<Double>("b"))
    }

    @Test
    fun `test write read close and re-read`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        val cryptoProvider = SimpleCryptoProvider()
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

        stash.put(data)

        stash.get<Data> {
            assertEquals("Hello, world!", it.a)
            assertEquals(120.94, it.b)
            assertEquals(3, it.c)
            assertTrue(it.d)
        }

        stash.close()

        stash = Stash(file, cryptoProvider)

        stash.get<Data> {
            assertEquals("Hello, world!", it.a)
            assertEquals(120.94, it.b)
            assertEquals(3, it.c)
            assertTrue(it.d)
        }
    }

    @Test
    fun `test write json and read as serializable`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        val cryptoProvider = SimpleCryptoProvider()
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

        stash.put(json)

        @Serializable
        data class Data(
            val a: String,
            val b: Double,
            val c: Int,
            val d: Boolean,
        )

        stash.get<Data> {
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
            SimpleCryptoProvider()
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

        stash.put(data)

        data.a = "Goodbye, world!"

        stash.put(data)

        stash.get<Data> {
            assertFalse(it.a == "Hello, world!")
            assertEquals("Goodbye, world!", it.a)
            assertEquals(120.94, it.b)
        }
    }

    @Test
    fun `test write edit and read`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            SimpleCryptoProvider()
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

        stash.put(data)

        stash.get<Data> {
            assertEquals("Hello, world!", it.a)
        }

        stash.edit<Data> {
            it.a = "Goodbye, world!"
        }

        stash.get<Data> {
            assertFalse(it.a == "Hello, world!")
            assertEquals("Goodbye, world!", it.a)
            assertEquals(120.94, it.b)
        }
    }

    @Test
    fun `test overwrite with different data`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            SimpleCryptoProvider()
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

        stash.put(data1)

        try {
            stash.put(data2)
        } catch (e: Throwable) {
            fail()
        }

        stash.get<Data2> {
            assertFalse(it.c == "Hello, world!")
            assertEquals("Goodbye, world!", it.c)
        }
    }

    @Test
    fun `test overwrite with same data in different types`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        val cryptoProvider = SimpleCryptoProvider()
        var stash = Stash(file, cryptoProvider)

        @Serializable
        data class Data1(
            val a: String,
            val b: Double,
        )

        val data1 = Data1("Hello, world!", 120.94)
        stash.put(data1)
        stash.close()

        stash = Stash(file, cryptoProvider)

        @Serializable
        data class Data2(
            val a: String,
            val b: Double,
        )

        stash.get<Data2> {
            assertEquals("Hello, world!", it.a)
            assertEquals(120.94, it.b)
        }
    }

    @Test
    fun `test write and read incorrectly typed property fails`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            SimpleCryptoProvider()
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

        stash.put(data)

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
            SimpleCryptoProvider()
        )

        data class Data(
            val a: String,
        )

        try {
            stash.put(Data("Hello, world!"))
            fail()
        } catch (e: IllegalArgumentException) {
            assertEquals("Must be @Serializable", e.message)
        }
    }

    @Test
    fun `test attempt to read non-serializable data fails`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            SimpleCryptoProvider()
        )

        data class Data(
            val a: String,
        )

        try {
            stash.get<Data> {
                fail()
            }
        } catch (e: IllegalArgumentException) {
            assertEquals("Must be @Serializable", e.message)
        }
    }

    @Test
    fun `test attempt to edit non-serializable data fails`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        val cryptoProvider = SimpleCryptoProvider()
        var stash = Stash(file, cryptoProvider)

        @Serializable
        data class SerializableData(
            val a: String,
            val b: Double,
        )

        val data = SerializableData(
            a = "Hello, world!",
            b = 120.94,
        )

        stash.put(data)

        stash.close()

        stash = Stash(file, cryptoProvider)

        data class NonSerializableData(
            val a: String,
            val b: Double,
        )

        try {
            stash.edit<NonSerializableData> {
                fail()
            }
        } catch (e: IllegalArgumentException) {
            assertEquals("Must be @Serializable", e.message)
        }
    }
}