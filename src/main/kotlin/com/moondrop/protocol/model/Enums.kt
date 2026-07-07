package com.moondrop.protocol.model

/**
 * ANC (主动降噪) 模式枚举。
 *
 * GAIA V3 协议中 ANC 模式通过 Feature=0x03, Cmd=0x04 设置，
 * 载荷为单字节模式值。
 */
enum class AncMode(val value: Byte, val label: String) {
    /** 普通模式 (关闭降噪) */
    NORMAL(0x01, "普通"),

    /** 主动降噪 */
    ANC(0x02, "降噪"),

    /** 通透模式 */
    TRANSPARENCY(0x04, "通透"),

    /** 自适应降噪 */
    ADAPTIVE(0x08, "自适应");

    companion object {
        private val map = entries.associateBy { it.value }

        /**
         * 从协议字节值解析 ANC 模式。
         * @return 对应的 AncMode，未知值返回 NORMAL
         */
        fun fromValue(value: Byte): AncMode = map[value] ?: NORMAL

        /**
         * 从名称字符串解析 ANC 模式。
         * @return 对应的 AncMode，未找到返回 NORMAL
         */
        fun fromName(name: String): AncMode = entries.find {
            it.name.equals(name, ignoreCase = true) || it.label == name
        } ?: NORMAL
    }
}

/**
 * Gain (增益) 级别枚举。
 *
 * GAIA V3 协议中 Gain 通过 Feature=0x1E, Cmd=0x02 设置。
 */
enum class GainLevel(val value: Byte, val label: String) {
    /** 高增益 */
    HIGH(0x00, "高"),

    /** 中增益 (默认) */
    MEDIUM(0x01, "中"),

    /** 低增益 */
    LOW(0x02, "低");

    companion object {
        private val map = entries.associateBy { it.value }

        /**
         * 从协议字节值解析 Gain 级别。
         * @return 对应的 GainLevel，未知值返回 MEDIUM
         */
        fun fromValue(value: Byte): GainLevel = map[value] ?: MEDIUM

        /**
         * 从名称字符串解析 Gain 级别。
         * @return 对应的 GainLevel，未找到返回 MEDIUM
         */
        fun fromName(name: String): GainLevel = entries.find {
            it.name.equals(name, ignoreCase = true) || it.label == name
        } ?: MEDIUM
    }
}
