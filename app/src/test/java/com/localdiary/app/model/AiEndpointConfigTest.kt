package com.localdiary.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiEndpointConfigTest {
    @Test
    fun `normalizedChatCompletionsUrl appends v1 and endpoint for host root`() {
        val config = AiEndpointConfig(
            baseUrl = "https://api.example.com",
            apiKey = "key",
            model = "test-model",
        )

        assertEquals(
            "https://api.example.com/v1/chat/completions",
            config.normalizedChatCompletionsUrl(),
        )
    }

    @Test
    fun `normalizedChatCompletionsUrl appends endpoint for v1 base url`() {
        val config = AiEndpointConfig(
            baseUrl = "https://api.example.com/v1/",
            apiKey = "key",
            model = "test-model",
        )

        assertEquals(
            "https://api.example.com/v1/chat/completions",
            config.normalizedChatCompletionsUrl(),
        )
    }

    @Test
    fun `normalizedChatCompletionsUrl keeps full endpoint`() {
        val config = AiEndpointConfig(
            baseUrl = "https://api.example.com/v1/chat/completions",
            apiKey = "key",
            model = "test-model",
        )

        assertEquals(
            "https://api.example.com/v1/chat/completions",
            config.normalizedChatCompletionsUrl(),
        )
    }

    @Test
    fun `normalizedImageChatCompletionsUrl appends v1 and endpoint for host root`() {
        val config = AiEndpointConfig(
            imageModelEnabled = true,
            imageBaseUrl = "https://vision.example.com",
            imageApiKey = "image-key",
            imageModel = "vision-model",
        )

        assertEquals(
            "https://vision.example.com/v1/chat/completions",
            config.normalizedImageChatCompletionsUrl(),
        )
    }

    @Test
    fun `isImageUnderstandingConfigured requires switch and all image fields`() {
        val disabled = AiEndpointConfig(
            imageModelEnabled = false,
            imageBaseUrl = "https://vision.example.com",
            imageApiKey = "image-key",
            imageModel = "vision-model",
        )
        val enabled = disabled.copy(imageModelEnabled = true)

        assertFalse(disabled.isImageUnderstandingConfigured)
        assertTrue(enabled.isImageUnderstandingConfigured)
    }
}
