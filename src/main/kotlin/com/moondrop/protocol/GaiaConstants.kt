package com.moondrop.protocol

/**
 * MOONDROP GAIA V3 协议常量定义。
 *
 * 基于 Qualcomm GAIA (Generic Audio Interface Architecture) V3 协议，
 * 通过 RFCOMM Channel 16 通信，Vendor ID = 29 (0x001D)。
 *
 * 参考来源:
 * - SpaceTravel-Protocol (GitHub) — Space Travel 抓包数据
 * - moondrop-spp-controller (GitHub) — Android SPP 控制实现
 * - MOONDROP Link APK 反编译
 */
object GaiaConstants {

    // ========== 传输层 ==========
    /** 标准 SPP UUID（GAIA V3 使用 RFCOMM Channel 16） */
    val SPP_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    /** GAIA Vendor ID = 29 (0x001D) */
    const val VENDOR_ID: Int = 0x001D

    /** TX 包头 */
    const val TX_HEADER_0: Byte = 0xFF.toByte()
    const val TX_HEADER_1: Byte = 0x04

    // ========== Feature ID ==========
    /** 设备管理 (电池, 配置查询) */
    const val FEATURE_DEVICE_MANAGEMENT: Byte = 0x00

    /** 基础功能 (固件版本, 状态, 设备状态) */
    const val FEATURE_BASIC_FUNCTION: Byte = 0x01

    /** ANC V2 (降噪控制) */
    const val FEATURE_ANC_V2: Byte = 0x03

    /** EQ/音乐处理 */
    const val FEATURE_EQ_MUSIC: Byte = 0x07

    /** 编解码器 (LDAC/LHDC/LC3) */
    const val FEATURE_CODEC: Byte = 0x0A

    /** 状态查询 */
    const val FEATURE_STATUS_QUERY: Byte = 0x0B

    /** Gain 控制 */
    const val FEATURE_GAIN: Byte = 0x1E

    // ========== Command ID — Feature 0x00 (设备管理) ==========
    /** 查询电池 */
    const val CMD_BATTERY: Byte = 0x01

    /** 查询配置 */
    const val CMD_CONFIG_QUERY: Byte = 0x0C

    /** 查询状态#19 */
    const val CMD_STATUS_QUERY_19: Byte = 0x13

    /** 查询状态#20 */
    const val CMD_STATUS_QUERY_20: Byte = 0x14

    /** 查询状态#21 */
    const val CMD_STATUS_QUERY_21: Byte = 0x15

    // ========== Command ID — Feature 0x01 (基础功能) ==========
    /** 固件版本 */
    const val CMD_FIRMWARE_VERSION: Byte = 0x05

    /** 状态查询 */
    const val CMD_STATUS_REQUEST: Byte = 0x07

    /** 配置响应 */
    const val CMD_CONFIG_RESPONSE: Byte = 0x0C

    /** 设备状态 */
    const val CMD_DEVICE_STATUS: Byte = 0x0D

    // ========== Command ID — Feature 0x03 (ANC V2) ==========
    /** 获取 ANC 模式 */
    const val CMD_ANC_GET_MODE: Byte = 0x03

    /** 设置 ANC 模式 */
    const val CMD_ANC_SET_MODE: Byte = 0x04

    // ========== Command ID — Feature 0x07 (EQ) ==========
    /** EQ 状态 */
    const val CMD_EQ_STATUS: Byte = 0x00

    /** 预设列表 */
    const val CMD_EQ_PRESET_LIST: Byte = 0x01

    /** 选择预设 */
    const val CMD_EQ_SELECT_PRESET: Byte = 0x03

    /** 获取用户配置 */
    const val CMD_EQ_GET_USER_CONFIG: Byte = 0x05

    /** 设置用户配置 */
    const val CMD_EQ_SET_USER_CONFIG: Byte = 0x06

    // ========== Command ID — Feature 0x0A (编解码器) ==========
    /** LDAC 状态查询 */
    const val CMD_CODEC_LDAC_STATUS: Byte = 0x02

    /** LHDC 状态查询 */
    const val CMD_CODEC_LHDC_STATUS: Byte = 0x03

    /** LDAC 激活/停用 */
    const val CMD_CODEC_LDAC_TOGGLE: Byte = 0x02

    /** LC3/LE Audio 开关 */
    const val CMD_CODEC_LC3_TOGGLE: Byte = 0x04

    // ========== Command ID — Feature 0x1E (Gain) ==========
    /** 获取增益 */
    const val CMD_GAIN_GET: Byte = 0x01

    /** 设置增益 */
    const val CMD_GAIN_SET: Byte = 0x02

    // ========== 设备名称 ==========
    /** 支持的品牌关键词 */
    val SUPPORTED_BRANDS = listOf("MOONDROP")

    /**
     * 检查设备名称是否匹配 MOONDROP 品牌。
     */
    fun isMoondropDevice(deviceName: String?): Boolean {
        if (deviceName.isNullOrBlank()) return false
        return SUPPORTED_BRANDS.any { deviceName.contains(it, ignoreCase = true) }
    }
}
