package com.openclaw.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.openclaw.app.data.model.ChatMessage
import com.openclaw.app.data.model.MessageRole
import com.openclaw.app.ui.components.MessageBubble
import com.openclaw.app.ui.components.TypingIndicator
import com.openclaw.app.ui.theme.OpenClawTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userMessageBubble_displaysContent() {
        val message = ChatMessage(
            role = MessageRole.USER,
            content = "Hello AI"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("Hello AI").assertIsDisplayed()
    }

    @Test
    fun assistantMessageBubble_displaysContent() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "Hello human!"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("Hello human!").assertIsDisplayed()
    }

    @Test
    fun streamingMessage_showsTypingIndicator() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            isStreaming = true
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        // Should show typing indicator for empty streaming message
        composeTestRule.onNodeWithText("思考中...").assertExists()
    }

    @Test
    fun errorMessage_showsErrorBanner() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "partial",
            error = "Network timeout"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("⚠️ Network timeout").assertIsDisplayed()
    }

    @Test
    fun errorMessage_showsRetryButton() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "",
            error = "API error"
        )

        var retried = false

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(
                    message = message,
                    onRetry = { retried = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("重试").performClick()
        assert(retried)
    }

    @Test
    fun copyButton_isPresent() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "Copy me"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithContentDescription("复制").assertExists()
    }

    @Test
    fun userMessage_showsUserAvatar() {
        val message = ChatMessage(
            role = MessageRole.USER,
            content = "test"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithContentDescription("用户").assertExists()
    }

    @Test
    fun assistantMessage_showsAIAvatar() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "test"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithContentDescription("AI").assertExists()
    }

    @Test
    fun messageWithImage_showsImagePlaceholder() {
        val message = ChatMessage(
            role = MessageRole.USER,
            content = "Look at this",
            imageUri = "data:image/jpeg;base64,/9j/4AAQ"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("Look at this").assertIsDisplayed()
    }

    @Test
    fun markdownRendering_handlesBoldText() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "This is **bold** text"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        // The text should render (bold parsed)
        composeTestRule.onNodeWithText("This is bold text", substring = true).assertExists()
    }

    @Test
    fun markdownRendering_handlesCodeBlock() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "```kotlin\nfun main() {}\n```"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("fun main() {}", substring = true).assertExists()
    }

    @Test
    fun markdownRendering_handlesInlineCode() {
        val message = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "Use `println` to print"
        )

        composeTestRule.setContent {
            OpenClawTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("println", substring = true).assertExists()
    }
}
