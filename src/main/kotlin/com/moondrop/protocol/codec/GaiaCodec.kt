package com.moondrop.protocol.codec

import com.moondrop.protocol.GaiaConstants
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 帧编解码器。
 *
 * 负责:
 * - TX: 将 GaiaPacket 编码为字节数组 (手机→耳机)
 * - RX: 从字节数组解析出 GaiaPacket (耳机→手机)
 */
object GaiaCodec {

    // ========== TX 编码 ==========

    /**
     * 编码 TX 命令包。
     *
     * 格式:
     * ```
     * FF 04 [lenHi] [lenLo] 00 [seq] [vendorLo] [vendorHi] [featureId] [cmdId] [payload...]
     * ```
     *
     * @param featureId Feature ID
     * @param cmdId Command ID
     * @param payload 载荷数据 (可选)
     * @param sequence 事务序号 (默认 0)
     * @return 编码后的字节数组
     */
    fun encode(
        featureId: Byte,
        cmdId: Byte,
        payload: ByteArray = byteArrayOf(),
        sequence: Byte = 0x00
    ): ByteArray {
        // len = featureId(1) + payload
        val len = 1 + payload.size
        val pkt = ByteArray(8 + 1 + payload.size) // header(8) + featureId(1) + payload

        pkt[0] = GaiaConstants.TX_HEADER_0
        pkt[1] = GaiaConstants.TX_HEADER_1
        pkt[2] = ((len shr 8) and 0xFF).toByte()
        pkt[3] = (len and 0xFF).toByte()
        pkt[4] = 0x00 // type
        pkt[5] = sequence
        pkt[6] = (GaiaConstants.VENDOR_ID and 0xFF).toByte()       // vendorLo
        pkt[7] = ((GaiaConstants.VENDOR_ID shr 8) and 0xFF).toByte() // vendorHi
        pkt[8] = featureId

        if (payload.isNotEmpty()) {
            System.arraycopy(payload, 0, pkt, 9, payload.size)
        }

        return pkt
    }

    /**
     * 从 GaiaPacket 对象编码为 TX 字节数组。
     */
    fun encode(packet: GaiaPacket): ByteArray {
        return encode(packet.featureId, packet.cmdId, packet.payload, packet.sequence)
    }

    // ========== RX 解码 ==========

    /**
     * 从 RX 字节数组解码出 GaiaPacket。
     *
     * RX 格式:
     * ```
     * [vendorLo] [vendorHi] [featureId] [cmdId] [payload...]
     * ```
     *
     * @param data 接收到的原始字节
     * @return 解码后的 GaiaPacket，数据无效返回 null
     */
    fun decode(data: ByteArray): GaiaPacket? {
        if (data.size < 4) return null

        val vendorId = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
        if (vendorId != GaiaConstants.VENDOR_ID) return null

        val featureId = data[2]
        val cmdId = data[3]
        val payload = if (data.size > 4) data.copyOfRange(4, data.size) else byteArrayOf()

        return GaiaPacket(
            featureId = featureId,
            cmdId = cmdId,
            payload = payload
        )
    }

    // ========== 流式解码 (用于 RFCOMM 流) ==========

    /**
     * 流式解码器状态。
     * 用于处理 TCP/RFCOMM 流中的 GAIA 包，支持分包和粘包。
     */
    class StreamDecoder {
        private val buffer = mutableListOf<Byte>()

        /**
         * 向解码器喂入数据。
         * @return 解码出的所有完整包
         */
        fun feed(data: ByteArray): List<GaiaPacket> {
            for (b in data) {
                buffer.add(b)
            }
            return drain()
        }

        /**
         * 尝试从缓冲区解码出所有完整包。
         */
        private fun drain(): List<GaiaPacket> {
            val packets = mutableListOf<GaiaPacket>()

            while (buffer.size >= 4) {
                // 查找 TX 包头 FF 04
                if (buffer.size >= 2 && buffer[0] == GaiaConstants.TX_HEADER_0 &&
                    buffer[1] == GaiaConstants.TX_HEADER_1
                ) {
                    // TX 格式: FF 04 [lenHi] [lenLo] ... 总长 = 8 + len
                    if (buffer.size < 4) break
                    val lenHi = buffer[2].toInt() and 0xFF
                    val lenLo = buffer[3].toInt() and 0xFF
                    val len = (lenHi shl 8) or lenLo
                    val totalLen = 8 + len

                    if (buffer.size < totalLen) break // 数据不足

                    // 提取 RX 部分 (跳过 TX 头)
                    val rxData = ByteArray(totalLen - 8)
                    for (i in rxData.indices) {
                        rxData[i] = buffer[8 + i]
                    }
                    // 移除已处理的数据
                    for (i in 0 until totalLen) {
                        buffer.removeAt(0)
                    }

                    decode(rxData)?.let { packets.add(it) }
                } else if (buffer[0].toInt() == 0x00) {
                    // RX 格式: [00] [vendorLo] [vendorHi] [featureId] [cmdId] ...
                    // 最小长度: 4 字节 (vendor + feature + cmd)
                    // 但实际 RX 包通常以 00 开头，后面是 vendor ID
                    if (buffer.size < 4) break

                    // 尝试解析
                    val rxData = ByteArray(buffer.size)
                    for (i in rxData.indices) {
                        rxData[i] = buffer[i]
                    }

                    val packet = decode(rxData)
                    if (packet != null) {
                        // 找到有效包，移除对应长度
                        val consumed = 4 + packet.payload.size
                        for (i in 0 until consumed.coerceAtMost(buffer.size)) {
                            buffer.removeAt(0)
                        }
                        packets.add(packet)
                    } else {
                        // 无效数据，跳过第一个字节
                        buffer.removeAt(0)
                    }
                } else {
                    // 未知格式，跳过
                    buffer.removeAt(0)
                }
            }

            return packets
        }

        /**
         * 清空缓冲区。
         */
        fun reset() {
            buffer.clear()
        }
    }
}
