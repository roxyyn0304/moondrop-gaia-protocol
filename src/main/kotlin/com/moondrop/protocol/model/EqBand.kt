package com.moondrop.protocol.model

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
