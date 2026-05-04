package com.openclaw.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.openclaw.app.ui.components.InputBar
import com.openclaw.app.ui.theme.OpenClawTheme
import org.junit.Rule
import org.junit.Test

class InputBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun inputBar_showsPlaceholder() {
        composeTestRule.setContent {
            OpenClawTheme {
                InputBar(
                    onSendMessage = {},
                    onStopGeneration = {},
                    onImagePicked = {},
                    isGenerating = false,
                    isListening = false,
                    pendingImageUri = null,
                    onRemoveImage = {},
                    onStartListening = {},
                    onStopListening = {}
                )
            }
        }

        composeTestRule.onNodeWithText("输入消息...").assertExists()
    }

    @Test
    fun inputBar_showsMicButtonWhenEmpty() {
        composeTestRule.setContent {
            OpenClawTheme {
                InputBar(
                    onSendMessage = {},
                    onStopGeneration = {},
                    onImagePicked = {},
                    isGenerating = false,
                    isListening = false,
                    pendingImageUri = null,
                    onRemoveImage = {},
                    onStartListening = {},
                    onStopListening = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("语音输入").assertExists()
    }

    @Test
    fun inputBar_showsImageButton() {
        composeTestRule.setContent {
            OpenClawTheme {
                InputBar(
                    onSendMessage = {},
                    onStopGeneration = {},
                    onImagePicked = {},
                    isGenerating = false,
                    isListening = false,
                    pendingImageUri = null,
                    onRemoveImage = {},
                    onStartListening = {},
                    onStopListening = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("添加图片").assertExists()
    }

    @Test
    fun inputBar_showsStopButtonWhenGenerating() {
        composeTestRule.setContent {
            OpenClawTheme {
                InputBar(
                    onSendMessage = {},
                    onStopGeneration = {},
                    onImagePicked = {},
                    isGenerating = true,
                    isListening = false,
                    pendingImageUri = null,
                    onRemoveImage = {},
                    onStartListening = {},
                    onStopListening = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("停止").assertExists()
    }

    @Test
    fun inputBar_sendButtonAppearsWhenTextEntered() {
        composeTestRule.setContent {
            OpenClawTheme {
                InputBar(
                    onSendMessage = {},
                    onStopGeneration = {},
                    onImagePicked = {},
                    isGenerating = false,
                    isListening = false,
                    pendingImageUri = null,
                    onRemoveImage = {},
                    onStartListening = {},
                    onStopListening = {}
                )
            }
        }

        // Type text
        composeTestRule.onNodeWithText("输入消息...").performTextInput("Hello")

        // Send button should appear
        composeTestRule.onNodeWithContentDescription("发送").assertExists()
    }

    @Test
    fun inputBar_imagePreview_showsWhenImageAttached() {
        composeTestRule.setContent {
            OpenClawTheme {
                InputBar(
                    onSendMessage = {},
                    onStopGeneration = {},
                    onImagePicked = {},
                    isGenerating = false,
                    isListening = false,
                    pendingImageUri = "data:image/jpeg;base64,abc",
                    onRemoveImage = {},
                    onStartListening = {},
                    onStopListening = {}
                )
            }
        }

        composeTestRule.onNodeWithText("图片已附加").assertExists()
    }

    @Test
    fun inputBar_listeningIndicator_showsWhenListening() {
        composeTestRule.setContent {
            OpenClawTheme {
                InputBar(
                    onSendMessage = {},
                    onStopGeneration = {},
                    onImagePicked = {},
                    isGenerating = false,
                    isListening = true,
                    pendingImageUri = null,
                    onRemoveImage = {},
                    onStartListening = {},
                    onStopListening = {}
                )
            }
        }

        composeTestRule.onNodeWithText("正在听...").assertExists()
    }
}
