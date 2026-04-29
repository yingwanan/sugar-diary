package com.localdiary.app.domain.psychology

import com.localdiary.app.model.PsychologyAgentPhase
import com.localdiary.app.model.PsychologyAgentProcessEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PsychologyAgentEventDisplayTest {
    @Test
    fun `visible events exclude internal synthesis profile and runtime result payloads`() {
        val visible = event("trigger", "触发情绪 Agent", PsychologyAgentPhase.ROUND_ONE, "专项分析", "用户可读内容")
        val critique = event("trigger", "触发情绪 Agent", PsychologyAgentPhase.CRITIQUE, "补充 / 反驳", "用户可读补充")
        val synthesis = event("synthesizer", "综合 Agent", PsychologyAgentPhase.SYNTHESIS, "最终综合", "{\"labels\":[]}")
        val profile = event("profile_updater", "画像更新 Agent", PsychologyAgentPhase.PROFILE, "画像更新", "{\"triggers\":[]}")
        val runtime = event("runtime_result", "运行结果", PsychologyAgentPhase.SYNTHESIS, "__RESULT__", "{\"labels\":[]}")

        val result = PsychologyAgentEventDisplay.visibleEvents(listOf(visible, synthesis, profile, runtime, critique))

        assertEquals(listOf(visible, critique), result)
    }

    @Test
    fun `synthesis status is shown only while synthesis is running`() {
        val synthesisPartial = event(
            agentId = "synthesizer",
            phase = PsychologyAgentPhase.SYNTHESIS,
            title = "最终综合",
            content = "{\"labels\"",
            isPartial = true,
        )
        val synthesisFinal = synthesisPartial.copy(isPartial = false)

        assertTrue(PsychologyAgentEventDisplay.shouldShowSynthesisStatus(listOf(synthesisPartial), working = true))
        assertFalse(PsychologyAgentEventDisplay.shouldShowSynthesisStatus(listOf(synthesisFinal), working = false))
    }

    private fun event(
        agentId: String,
        agentName: String = "综合 Agent",
        phase: PsychologyAgentPhase,
        title: String,
        content: String,
        isPartial: Boolean = false,
    ): PsychologyAgentProcessEvent = PsychologyAgentProcessEvent(
        id = "$agentId-$phase-$title-$isPartial",
        runId = "run",
        entryId = "entry",
        agentId = agentId,
        agentName = agentName,
        phase = phase,
        title = title,
        contentMarkdown = content,
        sequence = 1,
        createdAt = 1L,
        isPartial = isPartial,
    )
}
