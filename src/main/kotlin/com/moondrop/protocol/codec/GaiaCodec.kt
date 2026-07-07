package com.moondrop.protocol.codec

import com.moondrop.protocol.GaiaConstants
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 帧编解码器。
 *
 * TX 格式:
 * ```
 * FF 04 [typeHi] [typeLo] [seq] [vendorLo] [vendorHi] [cmdIdLo] [cmdIdHi] [payload...]
 * ```
 *
 * RX 格式:
 * ```
 * [00] [cmdSpaceLo] [cmdSpaceHi] [cmdIdLo] [cmdIdHi] [payload...]
 * ```
 */
object GaiaCodec {

    // ========== TX 编码 ==========

    /**
     * 编码 TX 命令包。
     *
     * @param cmdId cmdId (2 字节，Feature + Command 编码)
     * @param payload 载荷数据 (可选)
     * @param sequence 事务序号 (默认 0)
     * @return 编码后的字节数组
     */
    fun encode(cmdId: Int, payload: ByteArray = byteArrayOf(), sequence: Byte = 0x00): ByteArray {
        val pkt = ByteArray(9 + payload.size) // header(2) + type(2) + seq(1) + vendor(2) + cmdId(2) + payload

        pkt[0] = GaiaConstants.TX_HEADER_0
        pkt[1] = GaiaConstants.TX_HEADER_1
        pkt[2] = 0x00 // type high
        pkt[3] = 0x01 // type low
        pkt[4] = sequence
        pkt[5] = (GaiaConstants.VENDOR_ID and 0xFF).toByte()       // vendorLo
        pkt[6] = ((GaiaConstants.VENDOR_ID shr 8) and 0xFF).toByte() // vendorHi
        pkt[7] = (cmdId and 0xFF).toByte()         // cmdIdLo
        pkt[8] = ((cmdId shr 8) and 0xFF).toByte() // cmdIdHi

        if (payload.isNotEmpty()) {
            System.arraycopy(payload, 0, pkt, 9, payload.size)
        }

        return pkt
    }

    /**
     * 从 GaiaPacket 对象编码为 TX 字节数组。
     */
    fun encode(packet: GaiaPacket): ByteArray {
        return encode(packet.cmdId, packet.payload, packet.sequence)
    }

    // ========== RX 解码 ==========

    /**
     * 从 RX 字节数组解码出 GaiaPacket。
     *
     * RX 格式:
     * ```
     * [00] [cmdSpaceLo] [cmdSpaceHi] [cmdIdLo] [cmdIdHi] [payload...]
     * ```
     *
     * @param data 接收到的原始字节
     * @return 解码后的 GaiaPacket，数据无效返回 null
     */
    fun decode(data: ByteArray): GaiaPacket? {
        if (data.size < 6) return null

        // 检查 cmdSpace (bytes 1-2) 是否为 Vendor ID
        val cmdSpace = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        if (cmdSpace != GaiaConstants.VENDOR_ID) return null

        // cmdId (bytes 3-4, little-endian)
        val cmdId = (data[3].toInt() and 0xFF) or ((data[4].toInt() and 0xFF) shl 8)

        val payload = if (data.size > 5) data.copyOfRange(5, data.size) else byteArrayOf()

        return GaiaPacket(
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

            while (buffer.size >= 6) {
                // 查找 TX 包头 FF 04
                if (buffer.size >= 2 && buffer[0] == GaiaConstants.TX_HEADER_0 &&
                    buffer[1] == GaiaConstants.TX_HEADER_1
                ) {
                    // TX 格式: FF 04 [type(2)] [seq(1)] [vendor(2)] [cmdId(2)] [payload...]
                    // 最小长度: 9 字节 (header + type + seq + vendor + cmdId)
                    if (buffer.size < 9) break

                    // 提取 RX 部分 (跳过 TX 头)
                    val rxData = ByteArray(buffer.size - 5) // skip FF 04 + type(2) + seq(1)
                    for (i in rxData.indices) {
                        rxData[i] = buffer[5 + i]
                    }

                    val decodedPacket = decode(rxData)
                    if (decodedPacket != null) {
                        packets.add(decodedPacket)
                        // 移除已处理的数据 (整个 TX 包)
                        val consumed = 9 + decodedPacket.payload.size
                        for (i in 0 until consumed.coerceAtMost(buffer.size)) {
                            buffer.removeAt(0)
                        }
                    } else {
                        // 无法解析，跳过 header
                        for (i in 0 until 9.coerceAtMost(buffer.size)) {
                            buffer.removeAt(0)
                        }
                    }
                } else if (buffer[0].toInt() == 0x00) {
                    // RX 格式: [00] [cmdSpace(2)] [cmdId(2)] [payload...]
                    // 最小长度: 5 字节
                    if (buffer.size < 5) break

                    // 尝试解析
                    val rxData = ByteArray(buffer.size)
                    for (i in rxData.indices) {
                        rxData[i] = buffer[i]
                    }

                    val packet = decode(rxData)
                    if (packet != null) {
                        // 找到有效包，移除对应长度
                        val consumed = 5 + packet.payload.size
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
