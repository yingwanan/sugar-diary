package com.localdiary.app.domain.psychology

import com.localdiary.app.model.PsychologyAgentPhase
import com.localdiary.app.model.PsychologyAgentProcessEvent

object PsychologyAgentEventDisplay {
    fun visibleEvents(events: List<PsychologyAgentProcessEvent>): List<PsychologyAgentProcessEvent> =
        events.filterNot(::isInternalEvent)

    fun shouldShowSynthesisStatus(events: List<PsychologyAgentProcessEvent>, working: Boolean): Boolean =
        working && events.any { event ->
            event.phase == PsychologyAgentPhase.SYNTHESIS &&
                event.agentId == "synthesizer" &&
                event.isPartial
        }

    private fun isInternalEvent(event: PsychologyAgentProcessEvent): Boolean =
        event.agentId == "runtime_result" ||
            event.title == "__RESULT__" ||
            event.phase == PsychologyAgentPhase.SYNTHESIS ||
            event.phase == PsychologyAgentPhase.PROFILE
}
