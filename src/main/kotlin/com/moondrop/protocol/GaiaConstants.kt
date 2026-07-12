package com.moondrop.protocol

/**
 * MOONDROP GAIA V3 协议常量定义。
 *
 * 基于实测 MOONDROP Pudding (杰理芯片) 数据。
 *
 * TX/RX 格式 (相同):
 *   FF 04 [Len:2 BE] [00] [Vendor:1] [FeatureID:1] [CmdID:1] [Payload...]
 *   Len = payload.size (仅载荷长度)
 *   总包长 = 8 + Len
 *
 * 响应位: Feature ID 的 bit 0 (Feature | 0x01 = 响应)
 *
 * 参考来源:
 * - moondrop-spp-controller (GitHub)
 * - SpaceTravel-Protocol (GitHub)
 * - 实测 MOONDROP Pudding 数据 (2026-07)
 */
object GaiaConstants {

    // ========== 传输层 ==========
    val SPP_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    const val VENDOR_ID: Int = 0x001D
    const val HEADER_0: Byte = 0xFF.toByte()
    const val HEADER_1: Byte = 0x04

    // ========== Feature ID (实测) ==========
    /** 设备管理: EQ 预设选择, 配置查询 */
    const val FEATURE_DEVICE_MGMT: Int = 0x00
    /** 基础功能: 固件版本, 心跳 */
    const val FEATURE_BASIC: Int = 0x01
    /** 序列号 */
    const val FEATURE_SERIAL: Int = 0x03
    /** 固件版本 (实测响应 Feature=0x05) */
    const val FEATURE_FIRMWARE: Int = 0x05
    /** EQ 状态 */
    const val FEATURE_EQ: Int = 0x07
    /** 自定义 EQ */
    const val FEATURE_EQ_CUSTOM: Int = 0x0A
    /** 配置查询 */
    const val FEATURE_CONFIG: Int = 0x0C
    /** 设备状态 */
    const val FEATURE_DEVICE_STATE: Int = 0x0D
    /** ANC 降噪控制 (实测确认) */
    const val FEATURE_ANC: Int = 0x40
    /** Gain 增益控制 */
    const val FEATURE_GAIN: Int = 0x1E
    /** 编解码器 */
    const val FEATURE_CODEC: Int = 0x20
    /** 序列号 (ASCII) */
    const val FEATURE_SERIAL_ASCII: Int = 0x14
    /** 设备 ID (ASCII) */
    const val FEATURE_DEVICE_ID: Int = 0x15

    // ========== 命令 ID ==========
    // Feature 0x00: 设备管理
    const val CMD_EQ_SELECT: Int = 0x0C

    // Feature 0x01: 基础功能
    const val CMD_FIRMWARE_VERSION: Int = 0x05
    const val CMD_HEARTBEAT: Int = 0x07

    // Feature 0x05: 固件版本 (实测)
    const val CMD_FW_VERSION: Int = 0x00

    // Feature 0x0A: 自定义 EQ
    const val CMD_EQ_CUSTOM: Int = 0x07

    // Feature 0x0C: 配置查询
    const val CMD_CONFIG_QUERY: Int = 0x00

    // Feature 0x0D: 设备状态
    const val CMD_DEVICE_STATE: Int = 0x07

    // Feature 0x14: 序列号
    const val CMD_SERIAL: Int = 0x01

    // Feature 0x15: 设备 ID
    const val CMD_DEVICE_ID: Int = 0x00

    // Feature 0x40: ANC (实测确认)
    /** ANC 状态查询: Cmd=0x03 */
    const val CMD_ANC_QUERY: Int = 0x03
    /** ANC 模式设置: Cmd=0x04, Payload=[mode] */
    const val CMD_ANC_SET: Int = 0x04
    /** ANC 可用模式查询: Cmd=0x29 */
    const val CMD_ANC_AVAILABLE: Int = 0x29

    // Feature 0x1E: Gain
    const val CMD_GAIN_QUERY: Int = 0x01
    const val CMD_GAIN_SET: Int = 0x02

    // Feature 0x20: 编解码器
    const val CMD_LDAC_STATUS: Int = 0x02
    const val CMD_LC3_STATUS: Int = 0x01
    const val CMD_LC3_TOGGLE: Int = 0x04

    // ========== ANC 模式 (实测) ==========
    const val ANC_OFF: Byte = 0x00        // 关闭
    const val ANC_TRANSPARENCY: Byte = 0x02  // 通透
    const val ANC_NOISE_CANCEL: Byte = 0x04  // 降噪
    const val ANC_ADAPTIVE: Byte = 0x08   // 自适应 (降噪子模式)
    const val ANC_ANTI_WIND: Byte = 0x10  // 抗风噪 (降噪子模式)

    // ========== 响应位 ==========
    const val RESPONSE_BIT: Int = 0x01
    fun isResponse(featureId: Int): Boolean = (featureId and RESPONSE_BIT) != 0
    fun baseFeatureId(featureId: Int): Int = featureId and RESPONSE_BIT.inv()

    // ========== 设备名称 ==========
    val SUPPORTED_BRANDS = listOf("MOONDROP")
    fun isMoondropDevice(deviceName: String?): Boolean {
        if (deviceName.isNullOrBlank()) return false
        return SUPPORTED_BRANDS.any { deviceName.contains(it, ignoreCase = true) }
    }
}
