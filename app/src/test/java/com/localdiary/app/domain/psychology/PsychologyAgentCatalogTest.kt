package com.localdiary.app.domain.psychology

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PsychologyAgentCatalogTest {
    @Test
    fun `default catalog exposes eight unique professional agents`() {
        val agents = PsychologyAgentCatalog.defaultAgents

        assertEquals(8, agents.size)
        assertEquals(agents.size, agents.map { it.id }.toSet().size)
        assertTrue(agents.any { it.id == "trigger_emotion" && it.displayName.contains("触发") })
        assertTrue(agents.any { it.id == "body_stress" && it.displayName.contains("身体") })
        assertTrue(agents.any { it.id == "risk_safety" && it.displayName.contains("风险") })
    }

    @Test
    fun `selection resolves all agents by default and one agent for specialist mode`() {
        assertEquals(PsychologyAgentCatalog.defaultAgents, PsychologyAgentCatalog.resolveSelection(null))
        assertEquals(
            listOf("cognitive_pattern"),
            PsychologyAgentCatalog.resolveSelection("cognitive_pattern").map { it.id },
        )
    }
}
