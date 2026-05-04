package com.openclaw.app.data.api

import com.openclaw.app.data.model.ApiMessage
import com.openclaw.app.data.model.ContentPart
import com.openclaw.app.data.model.ImageUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OpenClawApiTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var api: OpenClawApi

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        api = OpenClawApi()
        api.updateConfig(
            endpoint = mockServer.url("/v1").toString().trimEnd('/'),
            apiKey = "test-key",
            model = "gpt-4o-mini"
        )
    }

    @After
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun `isConfigured returns true when endpoint and key set`() {
        assertTrue(api.isConfigured())
    }

    @Test
    fun `isConfigured returns false when endpoint blank`() {
        val emptyApi = OpenClawApi()
        assertFalse(emptyApi.isConfigured())
    }

    @Test
    fun `sendMessage returns success on valid response`() {
        val responseBody = """
        {
            "id": "chatcmpl-123",
            "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "Hello!"},
                "finish_reason": "stop"
            }]
        }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setBody(responseBody)
            .setHeader("Content-Type", "application/json"))

        val result = api.sendMessage(listOf(ApiMessage(role = "user", content = "Hi")))
        assertTrue(result.isSuccess)
        assertEquals("Hello!", result.getOrNull())
    }

    @Test
    fun `sendMessage returns failure on HTTP error`() {
        mockServer.enqueue(MockResponse()
            .setResponseCode(401)
            .setBody("""{"error":{"message":"Unauthorized","type":"auth_error"}}""")
            .setHeader("Content-Type", "application/json"))

        val result = api.sendMessage(listOf(ApiMessage(role = "user", content = "Hi")))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unauthorized") == true)
    }

    @Test
    fun `sendMessage returns failure on network error`() {
        mockServer.shutdown()

        val result = api.sendMessage(listOf(ApiMessage(role = "user", content = "Hi")))
        assertTrue(result.isFailure)
    }

    @Test
    fun `sendMessage returns failure when not configured`() {
        val emptyApi = OpenClawApi()
        val result = emptyApi.sendMessage(listOf(ApiMessage(role = "user", content = "Hi")))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("未配置") == true)
    }

    @Test
    fun `sendMessageStream calls onComplete on success`() {
        val sseData = """data: {"id":"1","choices":[{"delta":{"content":"Hi"}}]}

data: {"id":"1","choices":[{"delta":{"content":" there"}}]}

data: [DONE]

"""
        mockServer.enqueue(MockResponse()
            .setBody(sseData)
            .setHeader("Content-Type", "text/event-stream"))

        var result = ""
        var completed = false
        var error: String? = null

        api.sendMessageStream(
            messages = listOf(ApiMessage(role = "user", content = "Hello")),
            onChunk = { result += it },
            onComplete = { completed = true },
            onError = { error = it }
        )

        // Wait for async callback
        Thread.sleep(1000)

        assertTrue(completed)
        assertNull(error)
        assertEquals("Hi there", result)
    }

    @Test
    fun `sendMessageStream calls onError on HTTP error`() {
        mockServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("""{"error":{"message":"Server Error"}}""")
            .setHeader("Content-Type", "application/json"))

        var error: String? = null
        api.sendMessageStream(
            messages = listOf(ApiMessage(role = "user", content = "Hello")),
            onChunk = {},
            onComplete = {},
            onError = { error = it }
        )

        Thread.sleep(1000)

        assertNotNull(error)
        assertTrue(error!!.contains("Server Error"))
    }

    @Test
    fun `sendMessageStream calls error when not configured`() {
        val emptyApi = OpenClawApi()
        var error: String? = null

        emptyApi.sendMessageStream(
            messages = listOf(ApiMessage(role = "user", content = "Hi")),
            onChunk = {},
            onComplete = {},
            onError = { error = it }
        )

        assertNotNull(error)
        assertTrue(error!!.contains("未配置"))
    }

    @Test
    fun `updateConfig changes configuration`() {
        api.updateConfig("https://new.api.com/v1", "new-key", "gpt-4")
        assertTrue(api.isConfigured())
    }

    @Test
    fun `sendMessage with multimodal content works`() {
        val responseBody = """
        {
            "id": "chatcmpl-123",
            "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "I see the image!"},
                "finish_reason": "stop"
            }]
        }
        """.trimIndent()

        mockServer.enqueue(MockResponse()
            .setBody(responseBody)
            .setHeader("Content-Type", "application/json"))

        val messages = listOf(
            ApiMessage(
                role = "user",
                content = listOf(
                    ContentPart(type = "text", text = "What's this?"),
                    ContentPart(type = "image_url", image_url = ImageUrl(url = "data:image/jpeg;base64,abc123"))
                )
            )
        )

        val result = api.sendMessage(messages)
        assertTrue(result.isSuccess)
        assertEquals("I see the image!", result.getOrNull())
    }
}
