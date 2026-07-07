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

    // ========== 电池查询 ==========

    /**
     * 构造电池查询包 (Feature=0x00, Cmd=0x01)。
     * 载荷: [0x01, 0x02] (查询左右耳)
     */
    fun batteryQuery(): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_DEVICE_MANAGEMENT,
            cmdId = GaiaConstants.CMD_BATTERY,
            payload = byteArrayOf(0x01, 0x02)
        )
    }

    // ========== 固件版本 ==========

    /**
     * 构造固件版本查询包 (Feature=0x00, Cmd=0x05)。
     */
    fun firmwareQuery(): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_DEVICE_MANAGEMENT,
            cmdId = GaiaConstants.CMD_FIRMWARE_VERSION
        )
    }

    // ========== ANC 控制 ==========

    /**
     * 构造 ANC 模式查询包 (Feature=0x03, Cmd=0x03)。
     */
    fun ancQuery(): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_ANC_V2,
            cmdId = GaiaConstants.CMD_ANC_GET_MODE
        )
    }

    /**
     * 构造 ANC 模式设置包 (Feature=0x03, Cmd=0x04)。
     *
     * @param mode ANC 模式
     */
    fun ancSet(mode: AncMode): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_ANC_V2,
            cmdId = GaiaConstants.CMD_ANC_SET_MODE,
            payload = byteArrayOf(mode.value)
        )
    }

    // ========== EQ 控制 ==========

    /**
     * 构造 EQ 状态查询包 (Feature=0x07, Cmd=0x00)。
     */
    fun eqQuery(): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_EQ_MUSIC,
            cmdId = GaiaConstants.CMD_EQ_STATUS
        )
    }

    /**
     * 构造 EQ 预设列表查询包 (Feature=0x07, Cmd=0x01)。
     */
    fun eqPresetListQuery(): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_EQ_MUSIC,
            cmdId = GaiaConstants.CMD_EQ_PRESET_LIST
        )
    }

    /**
     * 构造 EQ 预设选择包 (Feature=0x07, Cmd=0x03)。
     *
     * @param presetId 预设 ID (0=默认, 63=自定义)
     */
    fun eqSelectPreset(presetId: Byte): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_EQ_MUSIC,
            cmdId = GaiaConstants.CMD_EQ_SELECT_PRESET,
            payload = byteArrayOf(presetId)
        )
    }

    /**
     * 构造自定义 EQ 设置包 (Feature=0x07, Cmd=0x06)。
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
            featureId = GaiaConstants.FEATURE_EQ_MUSIC,
            cmdId = GaiaConstants.CMD_EQ_SET_USER_CONFIG,
            payload = payload
        )
    }

    // ========== Gain 控制 ==========

    /**
     * 构造 Gain 查询包 (Feature=0x1E, Cmd=0x01)。
     */
    fun gainQuery(): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_GAIN,
            cmdId = GaiaConstants.CMD_GAIN_GET
        )
    }

    /**
     * 构造 Gain 设置包 (Feature=0x1E, Cmd=0x02)。
     *
     * @param level Gain 级别
     */
    fun gainSet(level: GainLevel): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_GAIN,
            cmdId = GaiaConstants.CMD_GAIN_SET,
            payload = byteArrayOf(level.value)
        )
    }

    // ========== 编解码器控制 ==========

    /**
     * 构造 LDAC 状态查询包 (Feature=0x0A, Cmd=0x02)。
     */
    fun ldacQuery(): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_CODEC,
            cmdId = GaiaConstants.CMD_CODEC_LDAC_STATUS
        )
    }

    /**
     * 构造 LHDC/LC3 状态查询包 (Feature=0x0A, Cmd=0x03)。
     */
    fun lc3Query(): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_CODEC,
            cmdId = GaiaConstants.CMD_CODEC_LHDC_STATUS
        )
    }

    /**
     * 构造 LDAC 激活/停用包 (Feature=0x0A, Cmd=0x02)。
     *
     * @param centralMac 中央设备 MAC 地址 (启用时需要)
     * @param enable true=激活, false=停用
     */
    fun ldacToggle(centralMac: String, enable: Boolean): GaiaPacket {
        if (!enable) {
            return GaiaPacket(
                featureId = GaiaConstants.FEATURE_CODEC,
                cmdId = GaiaConstants.CMD_CODEC_LDAC_TOGGLE,
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
            featureId = GaiaConstants.FEATURE_CODEC,
            cmdId = GaiaConstants.CMD_CODEC_LDAC_TOGGLE,
            payload = macBytes
        )
    }

    /**
     * 构造 LC3/LE Audio 开关包 (Feature=0x0A, Cmd=0x04)。
     *
     * @param enable true=开启, false=关闭
     */
    fun lc3Toggle(enable: Boolean): GaiaPacket {
        return GaiaPacket(
            featureId = GaiaConstants.FEATURE_CODEC,
            cmdId = GaiaConstants.CMD_CODEC_LC3_TOGGLE,
            payload = byteArrayOf(if (enable) 0x01 else 0x00)
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
