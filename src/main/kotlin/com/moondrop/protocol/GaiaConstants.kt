package com.moondrop.protocol

/**
 * MOONDROP GAIA V3 协议常量定义。
 *
 * 基于实测 MOONDROP Pudding (杰理芯片) btsnoop 抓包数据 (2026-07)。
 *
 * TX/RX 格式:
 *   FF 04 [Len:2 BE] [Seq:1] [Vendor:1] [FeatureID:1] [CmdID:1] [Payload...]
 *   Len = 1 + 1 + payload.size (feature + cmd + payload)
 *   总包长 = 8 + payload.size
 *
 * 响应位: Feature ID 的 bit 0 (Feature | 0x01 = 响应)
 *
 * 协议结构 (抓包确认):
 *   大部分查询: Feature=0x00, Cmd=功能号, 响应 Feature=0x01
 *   ANC 控制:   Feature=0x40, Cmd=子功能,  响应 Feature=0x41
 *   Gain 控制:  Feature=0x1E, Cmd=子功能,  响应 Feature=0x1F
 *   编解码器:   Feature=0x20, Cmd=子功能,  响应 Feature=0x21
 */
object GaiaConstants {

    // ========== 传输层 ==========
    val SPP_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    const val VENDOR_ID: Int = 0x001D
    const val HEADER_0: Byte = 0xFF.toByte()
    const val HEADER_1: Byte = 0x04

    // ========== Feature ID (抓包确认) ==========
    /** 基础查询 Feature (固件版本、序列号、设备ID、EQ、配置、设备状态等) */
    const val FEATURE_BASE: Int = 0x00
    /** ANC 降噪控制 */
    const val FEATURE_ANC: Int = 0x40
    /** Gain 增益控制 */
    const val FEATURE_GAIN: Int = 0x1E
    /** 编解码器 (LDAC/LC3) */
    const val FEATURE_CODEC: Int = 0x20
    /** 设备管理 (EQ预设选择等) */
    const val FEATURE_DEVICE_MGMT: Int = 0x1A

    // ========== Cmd ID - 基础查询 (Feature=0x00, 抓包确认) ==========
    /** 支持的命令列表 */
    const val CMD_SUPPORTED_COMMANDS: Int = 0x01
    /** 固件版本查询 */
    const val CMD_FIRMWARE_VERSION: Int = 0x05
    /** 心跳 */
    const val CMD_HEARTBEAT: Int = 0x07
    /** 设备状态 */
    const val CMD_DEVICE_STATE: Int = 0x0D
    /** 配置查询 (子命令在 payload 中) */
    const val CMD_CONFIG_QUERY: Int = 0x0C
    /** 序列号查询 */
    const val CMD_SERIAL: Int = 0x14
    /** 设备 ID 查询 */
    const val CMD_DEVICE_ID: Int = 0x15

    // ========== Cmd ID - ANC (Feature=0x40, 抓包确认) ==========
    /** ANC 状态查询 */
    const val CMD_ANC_QUERY: Int = 0x03
    /** ANC 模式设置: Payload=[mode] */
    const val CMD_ANC_SET: Int = 0x04
    /** ANC 可用模式查询 */
    const val CMD_ANC_AVAILABLE: Int = 0x29

    // ========== Cmd ID - Gain (Feature=0x1E, 抓包确认) ==========
    const val CMD_GAIN_QUERY: Int = 0x01
    const val CMD_GAIN_SET: Int = 0x02

    // ========== Cmd ID - 编解码器 (Feature=0x20, 抓包确认) ==========
    const val CMD_LDAC_STATUS: Int = 0x05
    const val CMD_LC3_STATUS: Int = 0x01

    // ========== Cmd ID - 设备管理 (Feature=0x1A, 抓包确认) ==========
    const val CMD_DEVICE_MGMT_QUERY: Int = 0x00
    const val CMD_DEVICE_MGMT_SET: Int = 0x01

    // ========== ANC 模式 (抓包确认) ==========
    const val ANC_OFF: Byte = 0x00
    const val ANC_TRANSPARENCY: Byte = 0x01
    const val ANC_NOISE_CANCEL: Byte = 0x02
    const val ANC_ADAPTIVE: Byte = 0x08
    const val ANC_ANTI_WIND: Byte = 0x10

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
