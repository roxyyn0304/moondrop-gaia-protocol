package com.moondrop.protocol.model

/**
 * ANC 模式枚举 (实测)。
 */
enum class AncMode(val value: Byte, val label: String) {
    OFF(0x00, "关闭"),
    TRANSPARENCY(0x02, "通透"),
    NOISE_CANCEL(0x04, "降噪"),
    ADAPTIVE(0x08, "自适应"),
    ANTI_WIND(0x10, "抗风噪");

    companion object {
        private val map = entries.associateBy { it.value }
        fun fromValue(value: Byte): AncMode = map[value] ?: OFF
    }
}

/**
 * Gain 级别枚举。
 */
enum class GainLevel(val value: Byte, val label: String) {
    HIGH(0x00, "高"),
    MEDIUM(0x01, "中"),
    LOW(0x02, "低");

    companion object {
        private val map = entries.associateBy { it.value }
        fun fromValue(value: Byte): GainLevel = map[value] ?: MEDIUM
    }
}
