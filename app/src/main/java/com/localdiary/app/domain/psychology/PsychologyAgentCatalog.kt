package com.localdiary.app.domain.psychology

data class PsychologyAgentDefinition(
    val id: String,
    val displayName: String,
    val focus: String,
    val prompt: String,
)

object PsychologyAgentCatalog {
    val defaultAgents: List<PsychologyAgentDefinition> = listOf(
        PsychologyAgentDefinition(
            id = "trigger_emotion",
            displayName = "触发情绪 Agent",
            focus = "识别触发事件、情绪链路和即时心理波动。",
            prompt = "专注分析具体触发点、情绪变化、强度来源和最直接的稳定建议。",
        ),
        PsychologyAgentDefinition(
            id = "cognitive_pattern",
            displayName = "认知模式 Agent",
            focus = "识别可能的认知偏差、解释风格和自动想法。",
            prompt = "专注分析可能的读心推测、灾难化、过度负责、全或无思维等认知模式，必须使用可能/倾向表述。",
        ),
        PsychologyAgentDefinition(
            id = "need_value",
            displayName = "需求价值 Agent",
            focus = "识别深层需求、价值感和未被满足的心理诉求。",
            prompt = "专注分析安全感、被理解、边界、掌控、休息、意义感等深层需求与价值冲突。",
        ),
        PsychologyAgentDefinition(
            id = "relationship_attachment",
            displayName = "关系依恋 Agent",
            focus = "识别关系互动、边界、依恋和支持系统线索。",
            prompt = "专注分析关系模式、边界张力、依恋线索、支持系统和互动压力，避免对他人做确定性判断。",
        ),
        PsychologyAgentDefinition(
            id = "defense_coping",
            displayName = "防御应对 Agent",
            focus = "识别防御机制、应对方式和短长期代价。",
            prompt = "专注分析可能的回避、合理化、压抑、讨好、投射、控制等防御或应对方式，并指出保护功能和代价。",
        ),
        PsychologyAgentDefinition(
            id = "body_stress",
            displayName = "身体压力 Agent",
            focus = "识别身体化压力、睡眠、能量和生理稳定线索。",
            prompt = "专注分析身体紧绷、疲劳、睡眠、饮食、呼吸、躯体压力信号，并给出低负担身体稳定建议。",
        ),
        PsychologyAgentDefinition(
            id = "strength_resource",
            displayName = "资源优势 Agent",
            focus = "识别心理资源、优势、韧性和可行动策略。",
            prompt = "专注识别用户已经呈现出的资源、价值、努力、边界尝试、复盘能力和可执行的下一步。",
        ),
        PsychologyAgentDefinition(
            id = "risk_safety",
            displayName = "风险安全 Agent",
            focus = "识别自伤、伤人、极端绝望或现实安全风险。",
            prompt = "专注识别安全风险。若有自伤、自杀、伤人或现实危险，只做温和提醒用户联系现实支持或当地紧急/专业资源，不做诊断。",
        ),
    )

    fun resolveSelection(selectedAgentId: String?): List<PsychologyAgentDefinition> =
        selectedAgentId
            ?.takeIf { it.isNotBlank() }
            ?.let { id -> defaultAgents.filter { it.id == id } }
            ?.takeIf { it.isNotEmpty() }
            ?: defaultAgents

    fun displayName(agentId: String): String = defaultAgents.firstOrNull { it.id == agentId }?.displayName ?: "心理 Agent"
}
