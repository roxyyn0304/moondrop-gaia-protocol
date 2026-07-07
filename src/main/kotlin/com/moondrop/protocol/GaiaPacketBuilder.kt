package com.moondrop.protocol

import com.moondrop.protocol.model.AncMode
import com.moondrop.protocol.model.GainLevel
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 命令包构造器。
 *
 * 提供类型安全的方法来构造各种常用命令包。
 */
object GaiaPacketBuilder {

    // ========== ANC 控制 ==========

    /**
     * 构造 ANC 模式查询包。
     */
    fun ancQuery(): GaiaPacket {
        return GaiaPacket(cmdId = GaiaConstants.CMD_ANC_QUERY)
    }

    /**
     * 构造 ANC 模式设置包。
     *
     * @param mode ANC 模式
     */
    fun ancSet(mode: AncMode): GaiaPacket {
        return GaiaPacket(
            cmdId = GaiaConstants.CMD_ANC_SET,
            payload = byteArrayOf(mode.value)
        )
    }

    // ========== Gain 控制 ==========

    /**
     * 构造 Gain 查询包。
     */
    fun gainQuery(): GaiaPacket {
        return GaiaPacket(cmdId = GaiaConstants.CMD_GAIN_QUERY)
    }

    /**
     * 构造 Gain 设置包。
     *
     * @param level Gain 级别
     */
    fun gainSet(level: GainLevel): GaiaPacket {
        return GaiaPacket(
            cmdId = GaiaConstants.CMD_GAIN_SET,
            payload = byteArrayOf(level.value)
        )
    }

    // ========== 编解码器控制 ==========

    /**
     * 构造 LDAC 状态查询包。
     */
    fun ldacQuery(): GaiaPacket {
        return GaiaPacket(cmdId = GaiaConstants.CMD_LDAC_QUERY)
    }

    /**
     * 构造 LHDC/LC3 状态查询包。
     */
    fun lc3Query(): GaiaPacket {
        return GaiaPacket(cmdId = GaiaConstants.CMD_LC3_QUERY)
    }

    /**
     * 构造 LDAC 激活/停用包。
     *
     * @param centralMac 中央设备 MAC 地址 (启用时需要)
     * @param enable true=激活, false=停用
     */
    fun ldacToggle(centralMac: String, enable: Boolean): GaiaPacket {
        if (!enable) {
            return GaiaPacket(
                cmdId = GaiaConstants.CMD_LDAC_TOGGLE,
                payload = byteArrayOf(0x00)
            )
        }

        val cleanMac = centralMac.replace(":", "").replace("-", "")
        if (cleanMac.length != 12) {
            throw IllegalArgumentException("Invalid MAC address: $centralMac")
        }

        val macBytes = ByteArray(6)
        for (i in 0 until 6) {
            macBytes[i] = cleanMac.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }

        return GaiaPacket(
            cmdId = GaiaConstants.CMD_LDAC_TOGGLE,
            payload = macBytes
        )
    }

    /**
     * 构造 LC3/LE Audio 开关包。
     *
     * @param enable true=开启, false=关闭
     */
    fun lc3Toggle(enable: Boolean): GaiaPacket {
        return GaiaPacket(
            cmdId = GaiaConstants.CMD_LC3_TOGGLE,
            payload = byteArrayOf(if (enable) 0x01 else 0x00)
        )
    }

    // ========== EQ 控制 ==========

    /**
     * 构造 EQ 状态查询包。
     */
    fun eqQuery(): GaiaPacket {
        return GaiaPacket(cmdId = GaiaConstants.CMD_EQ_QUERY)
    }

    /**
     * 构造 EQ 预设选择包。
     *
     * @param presetId 预设 ID
     */
    fun eqSelectPreset(presetId: Byte): GaiaPacket {
        return GaiaPacket(
            cmdId = GaiaConstants.CMD_EQ_SELECT,
            payload = byteArrayOf(presetId)
        )
    }

    /**
     * 构造自定义 EQ 设置包。
     *
     * @param preGain 前置增益 (dB)
     * @param bands 频段配置列表
     */
    fun eqSetCustom(preGain: Float, bands: List<EqBand>): GaiaPacket {
        if (bands.isEmpty()) return eqSelectPreset(0)

        val startBand = 0
        val endBand = bands.size - 1
        val payloadSize = 4 + bands.size * 7
        val payload = ByteArray(payloadSize)

        payload[0] = startBand.toByte()
        payload[1] = endBand.toByte()

        // preGain: float → Q10.6 定点数 (乘以 64)
        val formattedPreGain = (preGain * 64).toInt().coerceIn(-32768, 32767)
        payload[2] = ((formattedPreGain shr 8) and 0xFF).toByte()
        payload[3] = (formattedPreGain and 0xFF).toByte()

        for (i in bands.indices) {
            val band = bands[i]
            val offset = 4 + i * 7

            // 频率 (Hz): int16 大端
            payload[offset] = ((band.freq shr 8) and 0xFF).toByte()
            payload[offset + 1] = (band.freq and 0xFF).toByte()

            // Q 值: float → Q10.6 定点数
            val qVal = (band.q * 64).toInt().coerceIn(0, 65535)
            payload[offset + 2] = ((qVal shr 8) and 0xFF).toByte()
            payload[offset + 3] = (qVal and 0xFF).toByte()

            // 滤波器类型
            payload[offset + 4] = band.filterType.toByte()

            // 增益 (dB): float → Q10.6 定点数
            val gainVal = (band.gain * 64).toInt().coerceIn(-32768, 32767)
            payload[offset + 5] = ((gainVal shr 8) and 0xFF).toByte()
            payload[offset + 6] = (gainVal and 0xFF).toByte()
        }

        return GaiaPacket(
            cmdId = GaiaConstants.CMD_EQ_SELECT,
            payload = payload
        )
    }
}

/**
 * EQ 频段配置。
 *
 * @param freq 频率 (Hz)
 * @param q Q 值
 * @param filterType 滤波器类型 (0=Peak, 1=LowShelf, 2=HighShelf)
 * @param gain 增益 (dB)
 */
data class EqBand(
    val freq: Int,
    val q: Float = 1.0f,
    val filterType: Int = 0,
    val gain: Float = 0.0f
)
