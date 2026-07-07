package com.moondrop.protocol.model

import com.moondrop.protocol.GaiaConstants

/**
 * GAIA V3 数据包。
 *
 * 包含 TX (手机→耳机) 和 RX (耳机→手机) 两种格式的统一表示。
 *
 * TX 格式:
 * ```
 * FF 04 [type(2)] [seq(1)] [vendor(2)] [cmdId(2)] [payload...]
 * ```
 *
 * RX 格式:
 * ```
 * [00] [vendor(2)] [cmdId(2)] [payload...]
 * ```
 */
data class GaiaPacket(
    /** cmdId (2 字节，编码 Feature + Command) */
    val cmdId: Int,

    /** 载荷数据 */
    val payload: ByteArray = byteArrayOf(),

    /** 事务序号 (TX 用) */
    val sequence: Byte = 0x00
) {
    /** 载荷长度 */
    val payloadLength: Int get() = payload.size

    /** 是否为响应包 (bit 8 置位) */
    val isResponse: Boolean get() = (cmdId and GaiaConstants.RESPONSE_BIT) != 0

    /** 获取不含响应位的基础 cmdId */
    val baseCmdId: Int get() = cmdId and GaiaConstants.RESPONSE_BIT.inv()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GaiaPacket) return false
        return cmdId == other.cmdId &&
                payload.contentEquals(other.payload) &&
                sequence == other.sequence
    }

    override fun hashCode(): Int {
        var result = cmdId
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + sequence.hashCode()
        return result
    }

    override fun toString(): String {
        val payloadHex = payload.joinToString(" ") { String.format("%02X", it) }
        return "GaiaPacket(cmdId=0x${String.format("%04X", cmdId)}, " +
                "payload=[$payloadHex], " +
                "seq=$sequence)"
    }
}
