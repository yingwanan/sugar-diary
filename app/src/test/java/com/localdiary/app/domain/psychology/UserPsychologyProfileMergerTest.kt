package com.localdiary.app.domain.psychology

import com.localdiary.app.model.UserPsychologyProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPsychologyProfileMergerTest {
    @Test
    fun `merges generated profile updates without duplicating existing user edited facts`() {
        val current = UserPsychologyProfile(
            triggers = listOf("会议反馈"),
            cognitivePatterns = listOf("读心推测"),
            needs = listOf("被理解"),
            relationshipPatterns = listOf("职场边界紧张"),
            defensePatterns = listOf("回避"),
            bodyStressSignals = listOf("胸口紧"),
            strengths = listOf("愿意复盘"),
            riskNotes = emptyList(),
            updatedAt = 10L,
            userEditedAt = 9L,
        )
        val update = UserPsychologyProfileUpdate(
            triggers = listOf("会议反馈", "截止日期压力"),
            cognitivePatterns = listOf("灾难化"),
            needs = listOf("被理解", "掌控感"),
            relationshipPatterns = emptyList(),
            defensePatterns = listOf("合理化"),
            bodyStressSignals = listOf("肩颈紧绷"),
            strengths = listOf("愿意复盘"),
            riskNotes = listOf("暂无高风险"),
        )

        val merged = UserPsychologyProfileMerger.merge(current, update, now = 20L)

        assertEquals(listOf("会议反馈", "截止日期压力"), merged.triggers)
        assertEquals(listOf("读心推测", "灾难化"), merged.cognitivePatterns)
        assertEquals(9L, merged.userEditedAt)
        assertEquals(20L, merged.updatedAt)
        assertTrue(merged.riskNotes.contains("暂无高风险"))
    }
}
