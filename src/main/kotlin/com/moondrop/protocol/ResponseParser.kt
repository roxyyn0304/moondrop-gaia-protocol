package com.moondrop.protocol

import com.moondrop.protocol.model.AncMode
import com.moondrop.protocol.model.GainLevel
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 响应解析器。
 *
 * 解析耳机返回的响应包，提取有用信息。
 */
object ResponseParser {

    /**
     * 解析 ANC 模式响应。
     *
     * @param packet 响应包
     * @return ANC 模式，解析失败返回 NORMAL
     */
    fun parseAncMode(packet: GaiaPacket): AncMode {
        if (packet.payload.isEmpty()) return AncMode.NORMAL
        return AncMode.fromValue(packet.payload[0])
    }

    /**
     * 解析 Gain 级别响应。
     *
     * @param packet 响应包
     * @return Gain 级别，解析失败返回 MEDIUM
     */
    fun parseGainLevel(packet: GaiaPacket): GainLevel {
        if (packet.payload.isEmpty()) return GainLevel.MEDIUM
        return GainLevel.fromValue(packet.payload[0])
    }

    /**
     * 解析 LDAC 状态响应。
     *
     * @param packet 响应包
     * @return true=已启用, false=未启用, null=解析失败
     */
    fun parseLdacStatus(packet: GaiaPacket): Boolean? {
        if (packet.payload.isEmpty()) return null
        return packet.payload[0] == 0x01.toByte()
    }

    /**
     * 解析 LC3 状态响应。
     *
     * @param packet 响应包
     * @return true=已启用, false=未启用, null=解析失败
     */
    fun parseLc3Status(packet: GaiaPacket): Boolean? {
        if (packet.payload.isEmpty()) return null
        return packet.payload[0] == 0x01.toByte()
    }

    /**
     * 解析固件版本响应。
     *
     * @param packet 响应包
     * @return 固件版本字符串，解析失败返回 null
     */
    fun parseFirmwareVersion(packet: GaiaPacket): String? {
        if (packet.payload.isEmpty()) return null
        return try {
            String(packet.payload, Charsets.US_ASCII).trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析序列号响应。
     *
     * @param packet 响应包
     * @return 序列号字符串，解析失败返回 null
     */
    fun parseSerialNumber(packet: GaiaPacket): String? {
        if (packet.payload.size < 2) return null
        return try {
            String(packet.payload, 1, packet.payload.size - 1, Charsets.US_ASCII).trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析 EQ 预设选择响应。
     *
     * @param packet 响应包
     * @return 当前选中的预设 ID，解析失败返回 -1
     */
    fun parseEqPreset(packet: GaiaPacket): Int {
        if (packet.payload.isEmpty()) return -1
        return packet.payload[0].toInt() and 0xFF
    }

    /**
     * 解析通用响应状态。
     *
     * @param packet 响应包
     * @return true=成功, false=失败
     */
    fun isSuccess(packet: GaiaPacket): Boolean {
        return packet.payload.isEmpty() || packet.payload[0] == 0x00.toByte()
    }
}
