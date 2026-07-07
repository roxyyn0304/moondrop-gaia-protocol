package com.moondrop.protocol.model

/**
 * GAIA V3 数据包。
 *
 * 包含 TX (手机→耳机) 和 RX (耳机→手机) 两种格式的统一表示。
 *
 * TX 格式:
 * ```
 * FF 04 [lenHi] [lenLo] [type] [seq] [vendorLo] [vendorHi] [featureId] [cmdId] [payload...]
 * ```
 *
 * RX 格式:
 * ```
 * [vendorLo] [vendorHi] [featureId] [cmdId] [payload...]
 * ```
 */
data class GaiaPacket(
    /** Feature ID */
    val featureId: Byte,

    /** Command ID */
    val cmdId: Byte,

    /** 载荷数据 */
    val payload: ByteArray = byteArrayOf(),

    /** 事务序号 (TX 用) */
    val sequence: Byte = 0x00,

    /** 包类型: 0=COMMAND, 1=NOTIFICATION, 2=RESPONSE, 3=ERROR */
    val packetType: Byte = 0x00
) {
    /** 载荷长度 */
    val payloadLength: Int get() = payload.size

    /** 是否为响应包 (CMD 第 7 位为 1) */
    val isResponse: Boolean get() = (cmdId.toInt() and 0x80) != 0

    /** 获取不含响应位的基础 Command ID */
    val baseCmdId: Byte get() = (cmdId.toInt() and 0x7F).toByte()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GaiaPacket) return false
        return featureId == other.featureId && cmdId == other.cmdId &&
                payload.contentEquals(other.payload) && sequence == other.sequence
    }

    override fun hashCode(): Int {
        var result = featureId.hashCode()
        result = 31 * result + cmdId.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + sequence.hashCode()
        return result
    }

    override fun toString(): String {
        val payloadHex = payload.joinToString(" ") { String.format("%02X", it) }
        return "GaiaPacket(feature=0x${String.format("%02X", featureId)}, " +
                "cmd=0x${String.format("%02X", cmdId)}, " +
                "payload=[$payloadHex], " +
                "seq=$sequence)"
    }
}
