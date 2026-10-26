package it.palsoftware.pastiera.inputmethod

/** Pure settings-to-filter policy. Rules may strengthen one another, never weaken. */
internal object AccidentalKeyPressPolicy {
    fun configuration(
        isPhysicalKeyboard: Boolean,
        globalOverlapEnabled: Boolean,
        longPressThresholdMs: Long
    ): AccidentalKeyPressFilter.Configuration {
        val overlapRule = when {
            isPhysicalKeyboard && globalOverlapEnabled -> AccidentalKeyPressFilter.OverlapRule.ALL
            else -> AccidentalKeyPressFilter.OverlapRule.NONE
        }
        val numberRowPolicy = 
            AccidentalKeyPressFilter.NumberRowPolicy()
        return AccidentalKeyPressFilter.Configuration(
            overlapRule = overlapRule,
            numberRowPolicy = numberRowPolicy,
            longPressThresholdMs = longPressThresholdMs,
            numberRowRepeatEnabled = true
        )
    }
}
