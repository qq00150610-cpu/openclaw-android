package com.openclaw.app.data.model

import org.junit.Assert.*
import org.junit.Test

class ChatMessageTest {

    @Test
    fun `default message has unique id`() {
        val msg1 = ChatMessage(role = MessageRole.USER, content = "a")
        val msg2 = ChatMessage(role = MessageRole.USER, content = "a")
        assertNotEquals(msg1.id, msg2.id)
    }

    @Test
    fun `default message has timestamp`() {
        val before = System.currentTimeMillis()
        val msg = ChatMessage(role = MessageRole.USER, content = "test")
        val after = System.currentTimeMillis()

        assertTrue(msg.timestamp in before..after)
    }

    @Test
    fun `default streaming is false`() {
        val msg = ChatMessage(role = MessageRole.ASSISTANT, content = "test")
        assertFalse(msg.isStreaming)
    }

    @Test
    fun `default error is null`() {
        val msg = ChatMessage(role = MessageRole.USER, content = "test")
        assertNull(msg.error)
    }

    @Test
    fun `MessageRole values are correct`() {
        assertEquals("system", MessageRole.SYSTEM.value)
        assertEquals("user", MessageRole.USER.value)
        assertEquals("assistant", MessageRole.ASSISTANT.value)
    }

    @Test
    fun `copy preserves unchanged fields`() {
        val original = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "hello",
            isStreaming = true
        )
        val copy = original.copy(content = "world")

        assertEquals(original.id, copy.id)
        assertEquals(original.role, copy.role)
        assertEquals(original.timestamp, copy.timestamp)
        assertEquals(original.isStreaming, copy.isStreaming)
        assertEquals("world", copy.content)
    }

    @Test
    fun `imageUri defaults to null`() {
        val msg = ChatMessage(role = MessageRole.USER, content = "test")
        assertNull(msg.imageUri)
    }

    @Test
    fun `ChatRequest defaults`() {
        val req = ChatRequest(
            model = "gpt-4",
            messages = listOf(ApiMessage(role = "user", content = "hi"))
        )

        assertEquals(0.7f, req.temperature)
        assertEquals(4096, req.max_tokens)
        assertTrue(req.stream)
    }
}
