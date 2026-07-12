package com.moondrop.protocol

import com.moondrop.protocol.model.AncMode
import com.moondrop.protocol.model.GainLevel
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 响应解析器。
 * 基于实测 MOONDROP Pudding (杰理芯片) 数据。
 */
object ResponseParser {

    /** 解析固件版本 (Feature 0x05, payload=ASCII) */
    fun parseFirmwareVersion(packet: GaiaPacket): String? {
        if (packet.featureId != GaiaConstants.FEATURE_FIRMWARE) return null
        if (packet.payload.isEmpty()) return null
        return try { String(packet.payload, Charsets.US_ASCII).trim() } catch (_: Exception) { null }
    }

    /** 解析序列号 (Feature 0x14, payload=ASCII) */
    fun parseSerialNumber(packet: GaiaPacket): String? {
        if (packet.featureId != GaiaConstants.FEATURE_SERIAL_ASCII) return null
        if (packet.payload.isEmpty()) return null
        val data = if (packet.payload.size > 1) packet.payload.copyOfRange(1, packet.payload.size) else packet.payload
        return try { String(data, Charsets.US_ASCII).trim() } catch (_: Exception) { null }
    }

    /** 解析设备 ID (Feature 0x15, payload=ASCII) */
    fun parseDeviceId(packet: GaiaPacket): String? {
        if (packet.featureId != GaiaConstants.FEATURE_DEVICE_ID) return null
        if (packet.payload.isEmpty()) return null
        return try { String(packet.payload, Charsets.US_ASCII).trim() } catch (_: Exception) { null }
    }

    /** 解析 ANC 状态 (Feature 0x40, payload=[mode]) */
    fun parseAncMode(packet: GaiaPacket): AncMode? {
        if (GaiaConstants.baseFeatureId(packet.featureId) != GaiaConstants.FEATURE_ANC) return null
        if (packet.payload.isEmpty()) return null
        return AncMode.fromValue(packet.payload[0])
    }

    /** 解析 ANC 可用模式 (Feature 0x40, Cmd 0x29) */
    fun parseAncAvailableModes(packet: GaiaPacket): List<Int> {
        if (packet.payload.isEmpty()) return emptyList()
        return packet.payload.map { it.toInt() and 0xFF }
    }

    /** 解析 Gain 级别 (Feature 0x1E) */
    fun parseGainLevel(packet: GaiaPacket): GainLevel? {
        if (GaiaConstants.baseFeatureId(packet.featureId) != GaiaConstants.FEATURE_GAIN) return null
        if (packet.payload.isEmpty()) return null
        return GainLevel.fromValue(packet.payload[0])
    }

    /** 解析 LDAC 状态 */
    fun parseLdacStatus(packet: GaiaPacket): Boolean? {
        if (packet.featureId != (GaiaConstants.FEATURE_CODEC or 0x01)) return null
        if (packet.commandId != GaiaConstants.CMD_LDAC_STATUS) return null
        if (packet.payload.isEmpty()) return null
        return packet.payload[0] == 0x01.toByte()
    }

    /** 解析 LC3 状态 */
    fun parseLc3Status(packet: GaiaPacket): Boolean? {
        if (packet.featureId != (GaiaConstants.FEATURE_CODEC or 0x01)) return null
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
