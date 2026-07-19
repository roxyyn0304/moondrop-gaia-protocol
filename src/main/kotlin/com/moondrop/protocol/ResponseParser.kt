package com.moondrop.protocol

import com.moondrop.protocol.model.AncMode
import com.moondrop.protocol.model.GainLevel
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 响应解析器。
 * 基于实测 MOONDROP Pudding (杰理芯片) btsnoop 抓包数据。
 *
 * 基础查询响应 Feature=0x01, 通过 Cmd 区分功能。
 */
object ResponseParser {

    /** 解析固件版本 (Feature=0x01, Cmd=0x05, payload=ASCII) */
    fun parseFirmwareVersion(packet: GaiaPacket): String? {
        if (packet.featureId != (GaiaConstants.FEATURE_BASE or GaiaConstants.RESPONSE_BIT)) return null
        if (packet.commandId != GaiaConstants.CMD_FIRMWARE_VERSION) return null
        if (packet.payload.isEmpty()) return null
        return try { String(packet.payload, Charsets.US_ASCII).trim() } catch (_: Exception) { null }
    }

    /** 解析序列号 (Feature=0x01, Cmd=0x14, payload=ASCII) */
    fun parseSerialNumber(packet: GaiaPacket): String? {
        if (packet.featureId != (GaiaConstants.FEATURE_BASE or GaiaConstants.RESPONSE_BIT)) return null
        if (packet.commandId != GaiaConstants.CMD_SERIAL) return null
        if (packet.payload.isEmpty()) return null
        return try { String(packet.payload, Charsets.US_ASCII).trim() } catch (_: Exception) { null }
    }

    /** 解析设备 ID (Feature=0x01, Cmd=0x15, payload=ASCII) */
    fun parseDeviceId(packet: GaiaPacket): String? {
        if (packet.featureId != (GaiaConstants.FEATURE_BASE or GaiaConstants.RESPONSE_BIT)) return null
        if (packet.commandId != GaiaConstants.CMD_DEVICE_ID) return null
        if (packet.payload.isEmpty()) return null
        return try { String(packet.payload, Charsets.US_ASCII).trim() } catch (_: Exception) { null }
    }

    /** 解析 ANC 状态 (Feature=0x41, payload[0]=当前模式, 接受 query/set 响应) */
    fun parseAncMode(packet: GaiaPacket): AncMode? {
        if (GaiaConstants.baseFeatureId(packet.featureId) != GaiaConstants.FEATURE_ANC) return null
        if (packet.payload.isEmpty()) return null
        return AncMode.fromValue(packet.payload[0])
    }

    /** 解析 ANC 可用模式 (Feature=0x41, Cmd=0x29)
     *  返回 5 字节：关闭/自适应/通透/抗风噪/降噪 可用标志 (0x01=可用) */
    fun parseAncAvailableModes(packet: GaiaPacket): List<Int> {
        if (GaiaConstants.baseFeatureId(packet.featureId) != GaiaConstants.FEATURE_ANC) return emptyList()
        if (packet.commandId != GaiaConstants.CMD_ANC_AVAILABLE) return emptyList()
        if (packet.payload.isEmpty()) return emptyList()
        return packet.payload.map { it.toInt() and 0xFF }
    }

    /** 解析 Gain 级别 (Feature=0x1F, payload[0]=当前级别, 接受 query/set 响应) */
    fun parseGainLevel(packet: GaiaPacket): GainLevel? {
        if (GaiaConstants.baseFeatureId(packet.featureId) != GaiaConstants.FEATURE_GAIN) return null
        if (packet.payload.isEmpty()) return null
        return GainLevel.fromValue(packet.payload[0])
    }

    /** 解析 LDAC 状态 (Feature=0x21, Cmd=0x05) */
    fun parseLdacStatus(packet: GaiaPacket): Boolean? {
        if (packet.featureId != (GaiaConstants.FEATURE_CODEC or GaiaConstants.RESPONSE_BIT)) return null
        if (packet.commandId != GaiaConstants.CMD_LDAC_STATUS) return null
        if (packet.payload.isEmpty()) return null
        return packet.payload[0] == 0x01.toByte()
    }

    /** 解析 LC3 状态 (Feature=0x21, Cmd=0x01) */
    fun parseLc3Status(packet: GaiaPacket): Boolean? {
        if (packet.featureId != (GaiaConstants.FEATURE_CODEC or GaiaConstants.RESPONSE_BIT)) return null
        if (packet.commandId != GaiaConstants.CMD_LC3_STATUS) return null
        if (packet.payload.isEmpty()) return null
        return packet.payload[0] == 0x01.toByte()
    }

    fun isError(packet: GaiaPacket): Boolean {
        return packet.payload.isNotEmpty() && packet.payload[0] == 0xFE.toByte()
    }

    fun isSuccess(packet: GaiaPacket): Boolean {
        return packet.payload.isEmpty() || packet.payload[0] == 0x00.toByte()
    }
}
