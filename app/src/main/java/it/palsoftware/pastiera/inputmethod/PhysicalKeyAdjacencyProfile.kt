package it.palsoftware.pastiera.inputmethod

import android.view.KeyEvent

/** Stable physical key identity, independent of the character emitted by the keyboard firmware. */
@JvmInline
value class PhysicalKeyId(val value: String) {
    override fun toString(): String = value
}

/** Layout knowledge required by the proximity-based accidental-key filter. */
interface PhysicalKeyAdjacencyProfile {
    val profileId: String
    val numberRowKeys: Set<PhysicalKeyId>

    fun isAdjacent(first: PhysicalKeyId, second: PhysicalKeyId): Boolean

    fun resolveDirectKey(keyCode: Int, scanCode: Int): PhysicalKeyId?

    fun resolveNormalizedFirmwareOutput(keyCode: Int, metaState: Int): Set<PhysicalKeyId> =
        emptySet()
}

