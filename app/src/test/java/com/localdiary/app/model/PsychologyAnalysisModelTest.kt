package com.localdiary.app.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PsychologyAnalysisModelTest {
    @Test
    fun `default psychology template requires full professional analysis dimensions`() {
        val error = EmotionPromptTemplate.validate(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE)

        assertNull(error)
        assertTrue(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE.contains("\"triggers\""))
        assertTrue(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE.contains("\"cognitivePatterns\""))
        assertTrue(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE.contains("\"needs\""))
        assertTrue(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE.contains("\"relationshipSignals\""))
        assertTrue(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE.contains("\"defenseMechanisms\""))
        assertTrue(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE.contains("\"strengths\""))
        assertTrue(AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE.contains("不能替代心理咨询、医学诊断或危机干预"))
    }

    @Test
    fun `psychology analysis result decodes extended dimensions`() {
        val result = Json.decodeFromString<PsychologyAnalysisResult>(
            """
                {
                  "labels":["焦虑(7/10)"],
                  "intensity":70,
                  "summary":"正在承受关系压力。",
                  "suggestions":["先写下触发点"],
                  "safetyFlag":false,
                  "triggers":["同事反馈"],
                  "cognitivePatterns":["灾难化"],
                  "needs":["被理解"],
                  "relationshipSignals":["边界感紧张"],
                  "defenseMechanisms":["回避"],
                  "strengths":["能主动求助"]
                }
            """.trimIndent(),
        )

        assertEquals(listOf("同事反馈"), result.triggers)
        assertEquals(listOf("灾难化"), result.cognitivePatterns)
        assertEquals(listOf("被理解"), result.needs)
        assertEquals(listOf("边界感紧张"), result.relationshipSignals)
        assertEquals(listOf("回避"), result.defenseMechanisms)
        assertEquals(listOf("能主动求助"), result.strengths)
    }
}
