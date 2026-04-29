package com.localdiary.app.domain.psychology

object PsychologyLabelNormalizer {
    private val scoreSuffix = Regex("""\s*[（(]\s*\d+\s*/\s*10\s*[）)]\s*$""")

    fun normalize(label: String): String = label
        .trim()
        .replace(scoreSuffix, "")
        .trim()

    fun normalizeLabels(labels: List<String>): List<String> = labels
        .map { normalize(it) }
        .filter { it.isNotBlank() }
        .distinct()
}
