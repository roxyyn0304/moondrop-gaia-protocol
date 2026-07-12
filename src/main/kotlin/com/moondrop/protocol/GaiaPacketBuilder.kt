package com.moondrop.protocol

import com.moondrop.protocol.model.AncMode
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 命令包构造器。
 * 基于实测 MOONDROP Pudding (杰理芯片) 数据。
 */
object GaiaPacketBuilder {

    // ========== ANC 控制 (Feature 0x40) ==========

    /** 查询 ANC 状态 */
    fun ancQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_ANC, GaiaConstants.CMD_ANC_QUERY)

    /** 设置 ANC 模式 */
    fun ancSet(mode: Byte): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_ANC, GaiaConstants.CMD_ANC_SET, byteArrayOf(mode))

    /** 设置 ANC 通过 AncMode 枚举 */
    fun ancSet(mode: AncMode): GaiaPacket = ancSet(mode.value)

    /** 查询可用 ANC 模式 */
    fun ancAvailable(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_ANC, GaiaConstants.CMD_ANC_AVAILABLE)

    // ========== 固件版本 (Feature 0x05) ==========

    fun firmwareVersionQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_FIRMWARE, GaiaConstants.CMD_FW_VERSION)

    // ========== 序列号 (Feature 0x14) ==========

    fun serialNumberQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_SERIAL_ASCII, GaiaConstants.CMD_SERIAL)

    // ========== 设备 ID (Feature 0x15) ==========

    fun deviceIdQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_DEVICE_ID, 0x00)

    // ========== Gain (Feature 0x1E) ==========

    fun gainQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_GAIN, GaiaConstants.CMD_GAIN_QUERY)
    fun gainSet(level: Byte): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_GAIN, GaiaConstants.CMD_GAIN_SET, byteArrayOf(level))

    // ========== 编解码器 (Feature 0x20) ==========

    fun ldacStatusQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_CODEC, GaiaConstants.CMD_LDAC_STATUS)
    fun lc3StatusQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_CODEC, GaiaConstants.CMD_LC3_STATUS)
    fun lc3Toggle(enable: Boolean): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_CODEC, GaiaConstants.CMD_LC3_TOGGLE, byteArrayOf(if (enable) 0x01 else 0x00))

    // ========== 设备状态 (Feature 0x0D) ==========

    fun deviceStateQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_DEVICE_STATE, GaiaConstants.CMD_DEVICE_STATE)

    // ========== 配置查询 (Feature 0x0C) ==========

    fun configQuery(configId: Byte): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_CONFIG, GaiaConstants.CMD_CONFIG_QUERY, byteArrayOf(configId))

    // ========== 探测 ==========

    fun supportedCommandsQuery(): GaiaPacket = GaiaPacket(GaiaConstants.FEATURE_BASIC, 0x00)
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
