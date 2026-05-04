package com.openclaw.app.data.repository

import com.openclaw.app.data.model.ChatMessage
import com.openclaw.app.data.model.MessageRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConversationRepositoryTest {

    private lateinit var repo: ConversationRepository

    @Before
    fun setup() {
        repo = ConversationRepository()
    }

    @Test
    fun `init creates one conversation`() = runTest {
        val conversations = repo.conversations.first()
        assertEquals(1, conversations.size)
    }

    @Test
    fun `init sets current conversation`() = runTest {
        val current = repo.currentConversation.first()
        assertNotNull(current)
        assertTrue(current!!.messages.isEmpty())
    }

    @Test
    fun `createNewConversation adds to list`() = runTest {
        repo.createNewConversation()
        val conversations = repo.conversations.first()
        assertEquals(2, conversations.size)
    }

    @Test
    fun `createNewConversation sets it as current`() = runTest {
        val newConv = repo.createNewConversation()
        val current = repo.currentConversation.first()
        assertEquals(newConv.id, current?.id)
    }

    @Test
    fun `switchConversation changes current`() = runTest {
        val first = repo.currentConversation.first()!!
        val second = repo.createNewConversation()

        repo.switchConversation(first.id)
        val current = repo.currentConversation.first()
        assertEquals(first.id, current?.id)
    }

    @Test
    fun `addMessage adds to current conversation`() = runTest {
        val msg = ChatMessage(role = MessageRole.USER, content = "Hello")
        repo.addMessage(msg)

        val current = repo.currentConversation.first()!!
        assertEquals(1, current.messages.size)
        assertEquals("Hello", current.messages[0].content)
    }

    @Test
    fun `addMessage updates title from first user message`() = runTest {
        val msg = ChatMessage(role = MessageRole.USER, content = "这是一个测试消息")
        repo.addMessage(msg)

        val current = repo.currentConversation.first()!!
        assertEquals("这是一个测试消息", current.title)
    }

    @Test
    fun `addMessage truncates long title`() = runTest {
        val longText = "A".repeat(50)
        val msg = ChatMessage(role = MessageRole.USER, content = longText)
        repo.addMessage(msg)

        val current = repo.currentConversation.first()!!
        assertTrue(current.title.length <= 33) // 30 + "..."
        assertTrue(current.title.endsWith("..."))
    }

    @Test
    fun `updateLastMessage modifies the last message`() = runTest {
        repo.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = ""))
        repo.updateLastMessage("Hello World")

        val current = repo.currentConversation.first()!!
        assertEquals("Hello World", current.messages.last().content)
    }

    @Test
    fun `markLastMessageStreaming updates streaming state`() = runTest {
        repo.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = "test", isStreaming = true))
        repo.markLastMessageStreaming(false)

        val current = repo.currentConversation.first()!!
        assertFalse(current.messages.last().isStreaming)
    }

    @Test
    fun `markLastMessageError sets error`() = runTest {
        repo.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = ""))
        repo.markLastMessageError("Network error")

        val current = repo.currentConversation.first()!!
        assertEquals("Network error", current.messages.last().error)
        assertFalse(current.messages.last().isStreaming)
    }

    @Test
    fun `clearCurrentConversation removes all messages`() = runTest {
        repo.addMessage(ChatMessage(role = MessageRole.USER, content = "test"))
        repo.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = "reply"))
        repo.clearCurrentConversation()

        val current = repo.currentConversation.first()!!
        assertTrue(current.messages.isEmpty())
    }

    @Test
    fun `deleteConversation removes from list`() = runTest {
        val second = repo.createNewConversation()
        val secondId = second.id

        repo.deleteConversation(secondId)
        val conversations = repo.conversations.first()
        assertEquals(1, conversations.size)
        assertNotEquals(secondId, conversations[0].id)
    }

    @Test
    fun `deleteConversation creates new if all deleted`() = runTest {
        val current = repo.currentConversation.first()!!
        repo.deleteConversation(current.id)

        val conversations = repo.conversations.first()
        assertEquals(1, conversations.size)
        assertNotNull(repo.currentConversation.first())
    }

    @Test
    fun `deleteConversation switches to another if current deleted`() = runTest {
        val first = repo.currentConversation.first()!!
        repo.createNewConversation()

        repo.deleteConversation(first.id)
        val current = repo.currentConversation.first()!!
        assertNotEquals(first.id, current.id)
    }

    @Test
    fun `removeLastMessages removes correct count`() = runTest {
        repo.addMessage(ChatMessage(role = MessageRole.USER, content = "msg1"))
        repo.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = "msg2"))
        repo.addMessage(ChatMessage(role = MessageRole.USER, content = "msg3"))
        repo.addMessage(ChatMessage(role = MessageRole.ASSISTANT, content = "msg4"))

        repo.removeLastMessages(2)

        val current = repo.currentConversation.first()!!
        assertEquals(2, current.messages.size)
        assertEquals("msg1", current.messages[0].content)
        assertEquals("msg2", current.messages[1].content)
    }

    @Test
    fun `removeLastMessages does nothing if count exceeds size`() = runTest {
        repo.addMessage(ChatMessage(role = MessageRole.USER, content = "only one"))
        repo.removeLastMessages(5)

        val current = repo.currentConversation.first()!!
        assertEquals(1, current.messages.size)
    }

    @Test
    fun `conversations are sorted by updatedAt`() = runTest {
        val first = repo.currentConversation.first()!!
        Thread.sleep(10) // Ensure different timestamps
        val second = repo.createNewConversation()

        val conversations = repo.conversations.first()
        // second should be more recent
        assertTrue(conversations.find { it.id == second.id }!!.updatedAt >=
            conversations.find { it.id == first.id }!!.updatedAt)
    }
}
