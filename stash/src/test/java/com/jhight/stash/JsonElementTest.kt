package com.jhight.stash

import com.jhight.stash.crypto.Aes128CryptoProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

class JsonElementTest {
    @Test
    fun `test write and read json object`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

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

        stash.read<JsonElement> {
            assertEquals("Hello, world!", it.jsonObject["a"]?.jsonPrimitive?.content)
            assertEquals(120.94, it.jsonObject["b"]?.jsonPrimitive?.double)
            assertEquals(3, it.jsonObject["c"]?.jsonPrimitive?.int)
            assertTrue(it.jsonObject["d"]?.jsonPrimitive?.boolean ?: false)
        }
    }

    @Test
    fun `test write and read individual json elements`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        stash.write("a", "Hello, world!")
        stash.write("b", 120.94)
        stash.write("c", 3)
        stash.write("d", true)

        assertEquals("Hello, world!", stash.get<String>("a"))
        assertEquals(120.94, stash.get<Double>("b"))
        assertEquals(3, stash.get<Int>("c"))
        assertTrue(stash.get<Boolean>("d") ?: false)
    }

    @Test
    fun `test write and read json arrays`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        val json = Json.parseToJsonElement(
            """
                {
                    "a": "Hello, world!",
                    "b": [1, 2, 3],
                    "c": [true, false, true]
                }
            """.trimIndent()
        )

        stash.write(json)

        assertEquals("Hello, world!", stash.get<String>("a"))
        assertEquals(listOf(1, 2, 3), stash.get<List<Int>>("b"))
        assertEquals(listOf(true, false, true), stash.get<List<Boolean>>("c"))
    }

    @Test
    fun `test write and read nested json objects of same type`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        val json = Json.parseToJsonElement(
            """
                {
                    "a": "Hello, world!",
                    "b": {
                        "x": 120,
                        "y": 3
                    },
                    "c": {
                        "z": true
                    }
                }
            """.trimIndent()
        )

        stash.write(json)

        assertEquals("Hello, world!", stash.get<String>("a"))
        assertEquals(mapOf("x" to 120, "y" to 3), stash.get<Map<String, Int>>("b"))
        assertEquals(mapOf("z" to true), stash.get<Map<String, Boolean>>("c"))
    }

    @Test
    fun `test write read close and re-read`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        val cryptoProvider = Aes128CryptoProvider()
        var stash = Stash(file, cryptoProvider)

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

        stash.read<JsonElement> {
            assertEquals("Hello, world!", it.string("a"))
            assertEquals(120.94, it.double("b"))
            assertEquals(3, it.int("c"))
            assertTrue(it.boolean("d") ?: false)
        }

        stash.close()

        stash = Stash(file, cryptoProvider)

        stash.read<JsonElement> {
            assertEquals("Hello, world!", it.string("a"))
            assertEquals(120.94, it.double("b"))
            assertEquals(3, it.int("c"))
            assertTrue(it.boolean("d") ?: false)
        }
    }

    @Test
    fun `test write edit and read`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        val json = Json.parseToJsonElement(
            """
                {
                    "a": "Hello, world!",
                    "b": 120.94
                }
            """.trimIndent()
        )

        stash.write(json)

        stash.write(json.jsonObject.edit {
            it["a"] = JsonPrimitive("Goodbye, world!")
        })

        stash.read<JsonElement> {
            assertFalse(it.string("a") == "Hello, world!")
            assertEquals("Goodbye, world!", it.string("a"))
            assertEquals(120.94, it.double("b"))
        }
    }

    @Test
    fun `test overwrite with different data`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            Aes128CryptoProvider()
        )

        val json1 = Json.parseToJsonElement(
            """
                {
                    "a": "Hello, world!",
                    "b": 120.94
                }
            """.trimIndent()
        )

        val json2 = Json.parseToJsonElement(
            """
                {
                    "c": "Goodbye, world!"
                }
            """.trimIndent()
        )

        stash.write(json1)

        try {
            stash.write(json2)
        } catch (e: Throwable) {
            fail()
        }

        stash.read<JsonElement> {
            assertNull(it.string("a"))
            assertNull(it.double("b"))
            assertFalse(it.string("c") == "Hello, world!")
            assertEquals("Goodbye, world!", it.string("c"))
        }
    }
}