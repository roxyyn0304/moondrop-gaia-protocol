package com.moondrop.protocol

/**
 * MOONDROP GAIA V3 协议常量定义。
 *
 * 基于 Qualcomm GAIA (Generic Audio Interface Architecture) V3 协议，
 * 通过 RFCOMM Channel 16 通信，Vendor ID = 29 (0x001D)。
 *
 * cmdId 是 2 字节值，编码了 Feature ID + Packet Type + Command ID：
 * - Bits 15-9: Feature ID (7 bits)
 * - Bits 8-7: Packet Type (2 bits, 0=COMMAND, 2=RESPONSE)
 * - Bits 6-0: Command ID (7 bits)
 *
 * 参考来源:
 * - SpaceTravel-Protocol (GitHub)
 * - moondrop-spp-controller (GitHub) — 实际工作的 cmdId 值
 * - MOONDROP Link APK 反编译
 */
object GaiaConstants {

    // ========== 传输层 ==========
    /** 标准 SPP UUID */
    val SPP_UUID: java.util.UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    /** GAIA Vendor ID = 29 (0x001D) */
    const val VENDOR_ID: Int = 0x001D

    /** TX 包头 */
    const val TX_HEADER_0: Byte = 0xFF.toByte()
    const val TX_HEADER_1: Byte = 0x04

    // ========== cmdId 常量 (2 字节，Feature + Command 编码) ==========
    // 从 moondrop-spp-controller 逆向得到的实际值

    // --- ANC (Feature=0x10) ---
    /** ANC 状态查询 (TX) */
    const val CMD_ANC_QUERY: Int = 0x1003
    /** ANC 状态响应 (RX) */
    const val CMD_ANC_RESPONSE: Int = 0x1103
    /** ANC 模式设置 (TX) */
    const val CMD_ANC_SET: Int = 0x1004

    // --- Gain (Feature=0x1E) ---
    /** Gain 查询 (TX) */
    const val CMD_GAIN_QUERY: Int = 0x1E01
    /** Gain 响应 (RX) */
    const val CMD_GAIN_RESPONSE: Int = 0x1F01
    /** Gain 设置 (TX) */
    const val CMD_GAIN_SET: Int = 0x1E02

    // --- 编解码器 (Feature=0x20) ---
    /** LDAC 状态查询 (TX) */
    const val CMD_LDAC_QUERY: Int = 0x2002
    /** LDAC 状态响应 (RX) */
    const val CMD_LDAC_RESPONSE: Int = 0x2102
    /** LDAC 激活/停用 (TX) */
    const val CMD_LDAC_TOGGLE: Int = 0x2A02

    /** LC3 状态查询 (TX) */
    const val CMD_LC3_QUERY: Int = 0x2001
    /** LC3 状态响应 (RX) */
    const val CMD_LC3_RESPONSE: Int = 0x2101
    /** LC3/LE Audio 开关 (TX) */
    const val CMD_LC3_TOGGLE: Int = 0x2004

    // --- EQ (Feature=0x0B) ---
    /** EQ 状态查询 (TX) */
    const val CMD_EQ_QUERY: Int = 0x0B02
    /** EQ 状态响应 (RX) */
    const val CMD_EQ_RESPONSE: Int = 0x0B02
    /** EQ 预设选择 (TX) */
    const val CMD_EQ_SELECT: Int = 0x0B03
    /** EQ 面板/详情响应 (RX) */
    const val CMD_EQ_DETAIL_RESPONSE: Int = 0x0B05

    // --- 设备信息 ---
    /** 固件版本响应 (RX) */
    const val CMD_FIRMWARE_RESPONSE: Int = 0x0105
    /** 序列号响应 (RX) */
    const val CMD_SERIAL_RESPONSE: Int = 0x0302
    /** 心跳/通用 ACK (RX) */
    const val CMD_HEARTBEAT: Int = 0x0107

    // ========== 响应位掩码 ==========
    /** 响应位 (bit 8) */
    const val RESPONSE_BIT: Int = 0x0100

    /**
     * 从 TX cmdId 计算对应的 RX 响应 cmdId。
     * 响应 = TX cmdId | RESPONSE_BIT
     */
    fun toResponseCmdId(txCmdId: Int): Int = txCmdId or RESPONSE_BIT

    /**
     * 检查 cmdId 是否为响应包。
     */
    fun isResponse(cmdId: Int): Boolean = (cmdId and RESPONSE_BIT) != 0

    /**
     * 获取 cmdId 的基础值（去掉响应位）。
     */
    fun baseCmdId(cmdId: Int): Int = cmdId and RESPONSE_BIT.inv()

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
