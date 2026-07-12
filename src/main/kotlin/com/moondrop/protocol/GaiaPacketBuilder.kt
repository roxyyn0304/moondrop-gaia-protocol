package com.moondrop.protocol

import com.moondrop.protocol.model.AncMode
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 命令包构造器。
 * 基于实测 MOONDROP Pudding (杰理芯片) btsnoop 抓包数据。
 *
 * 协议结构: Feature=0x00 用于基础查询, Cmd 为功能号。
 * ANC/Gain/Codec 使用独立 Feature ID。
 */
object GaiaPacketBuilder {

    // ========== ANC 控制 (Feature 0x40) ==========

    fun ancQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_ANC, GaiaConstants.CMD_ANC_QUERY)

    fun ancSet(mode: Byte): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_ANC, GaiaConstants.CMD_ANC_SET, byteArrayOf(mode))

    fun ancSet(mode: AncMode): GaiaPacket = ancSet(mode.value)

    fun ancAvailable(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_ANC, GaiaConstants.CMD_ANC_AVAILABLE)

    // ========== 基础查询 (Feature 0x00, Cmd=功能号) ==========

    fun firmwareVersionQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_BASE, GaiaConstants.CMD_FIRMWARE_VERSION)

    fun serialNumberQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_BASE, GaiaConstants.CMD_SERIAL)

    fun deviceIdQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_BASE, GaiaConstants.CMD_DEVICE_ID)

    fun deviceStateQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_BASE, GaiaConstants.CMD_DEVICE_STATE)

    fun configQuery(configId: Byte): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_BASE, GaiaConstants.CMD_CONFIG_QUERY, byteArrayOf(configId))

    fun supportedCommandsQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_BASE, GaiaConstants.CMD_SUPPORTED_COMMANDS)

    // ========== Gain (Feature 0x1E) ==========

    fun gainQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_GAIN, GaiaConstants.CMD_GAIN_QUERY)
    fun gainSet(level: Byte): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_GAIN, GaiaConstants.CMD_GAIN_SET, byteArrayOf(level))

    // ========== 编解码器 (Feature 0x20) ==========

    fun ldacStatusQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_CODEC, GaiaConstants.CMD_LDAC_STATUS)
    fun lc3StatusQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_CODEC, GaiaConstants.CMD_LC3_STATUS)

    // ========== 探测 ==========

    fun probe(featureId: Int, cmdId: Int): GaiaPacket = GaiaPacket(featureId, cmdId)

    // ========== 批量查询 ==========

    fun statusQueryPackets(): List<GaiaPacket> = listOf(
        supportedCommandsQuery(),
        firmwareVersionQuery(),
        ancQuery(),
        gainQuery(),
        serialNumberQuery(),
        deviceIdQuery()
    )
}
