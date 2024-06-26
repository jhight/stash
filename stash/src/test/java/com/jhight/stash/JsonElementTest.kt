package com.jhight.stash

import com.jhight.stash.crypto.SimpleCryptoProvider
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
            SimpleCryptoProvider()
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

        stash.put(json)

        stash.get<JsonElement> {
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
            SimpleCryptoProvider()
        )

        stash.put("a", "Hello, world!")
        stash.put("b", 120.94)
        stash.put("c", 3)
        stash.put("d", true)
        stash.put("e", listOf(1, 2, 3))
        stash.put("f", mapOf("x" to 1, "y" to 2, "z" to 3))
        stash.put("g", Json.parseToJsonElement("{ \"x\": 1, \"y\": 2, \"z\": 3 }"))
        stash.put<String?>("h", null)

        assertEquals("Hello, world!", stash.get<String>("a"))
        assertEquals(120.94, stash.get<Double>("b"))
        assertEquals(3, stash.get<Int>("c"))
        assertTrue(stash.get<Boolean>("d") ?: false)
        assertEquals(listOf(1, 2, 3), stash.get<List<Int>>("e"))
        assertEquals(mapOf("x" to 1, "y" to 2, "z" to 3), stash.get<Map<String, Int>>("f"))
        assertEquals(mapOf("x" to 1, "y" to 2, "z" to 3), stash.get<Map<String, Int>>("g"))
        assertNull(stash.get<String?>("h"))
    }

    @Test
    fun `test write and read json arrays`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            SimpleCryptoProvider()
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

        stash.put(json)

        assertEquals("Hello, world!", stash.get<String>("a"))
        assertEquals(listOf(1, 2, 3), stash.get<List<Int>>("b"))
        assertEquals(listOf(true, false, true), stash.get<List<Boolean>>("c"))
    }

    @Test
    fun `test write and read nested json objects of same type`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            SimpleCryptoProvider()
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

        stash.put(json)

        assertEquals("Hello, world!", stash.get<String>("a"))
        assertEquals(mapOf("x" to 120, "y" to 3), stash.get<Map<String, Int>>("b"))
        assertEquals(mapOf("z" to true), stash.get<Map<String, Boolean>>("c"))
    }

    @Test
    fun `test write read close and re-read`() = runTest {
        val file = File("/tmp/" + System.nanoTime() + ".stash")
        val cryptoProvider = SimpleCryptoProvider()
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

        stash.put(json)

        stash.get<JsonElement> {
            assertEquals("Hello, world!", it.string("a"))
            assertEquals(120.94, it.double("b"))
            assertEquals(3, it.int("c"))
            assertTrue(it.boolean("d") ?: false)
        }

        stash.close()

        stash = Stash(file, cryptoProvider)

        stash.get<JsonElement> {
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
            SimpleCryptoProvider()
        )

        val json = Json.parseToJsonElement(
            """
                {
                    "a": "Hello, world!",
                    "b": 120.94
                }
            """.trimIndent()
        )

        stash.put(json)

        stash.put(json.jsonObject.edit {
            it["a"] = JsonPrimitive("Goodbye, world!")
        })

        stash.get<JsonElement> {
            assertFalse(it.string("a") == "Hello, world!")
            assertEquals("Goodbye, world!", it.string("a"))
            assertEquals(120.94, it.double("b"))
        }
    }

    @Test
    fun `test overwrite with different data`() = runTest {
        val stash = Stash(
            File("/tmp/" + System.nanoTime() + ".stash"),
            SimpleCryptoProvider()
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

        stash.put(json1)

        try {
            stash.put(json2)
        } catch (e: Throwable) {
            fail()
        }

        stash.get<JsonElement> {
            assertNull(it.string("a"))
            assertNull(it.double("b"))
            assertFalse(it.string("c") == "Hello, world!")
            assertEquals("Goodbye, world!", it.string("c"))
        }
    }
}