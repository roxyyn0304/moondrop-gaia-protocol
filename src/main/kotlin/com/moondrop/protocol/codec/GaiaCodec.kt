package com.moondrop.protocol.codec

import com.moondrop.protocol.GaiaConstants
import com.moondrop.protocol.model.GaiaPacket

/**
 * GAIA V3 帧编解码器。
 *
 * 包格式 (TX 和 RX 相同):
 * ```
 * FF 04 [Len:2 BE] [Seq:1] [Vendor:1] [FeatureID:1] [CmdID:1] [Payload...]
 * ```
 * Len = 1 + 1 + payload.size (feature + cmd + payload)
 * 总包长 = 8 + payload.size
 */
object GaiaCodec {

    // ========== 编码 ==========

    fun encode(featureId: Int, commandId: Int, payload: ByteArray = byteArrayOf(), sequence: Byte = 0x00): ByteArray {
        val pkt = ByteArray(8 + payload.size)
        pkt[0] = GaiaConstants.HEADER_0
        pkt[1] = GaiaConstants.HEADER_1
        // Len: big-endian, payload size only
        pkt[2] = ((payload.size shr 8) and 0xFF).toByte()
        pkt[3] = (payload.size and 0xFF).toByte()
        pkt[4] = sequence
        pkt[5] = (GaiaConstants.VENDOR_ID and 0xFF).toByte()
        pkt[6] = (featureId and 0xFF).toByte()
        pkt[7] = (commandId and 0xFF).toByte()
        if (payload.isNotEmpty()) {
            System.arraycopy(payload, 0, pkt, 8, payload.size)
        }
        return pkt
    }

    fun encode(packet: GaiaPacket): ByteArray {
        return encode(packet.featureId, packet.commandId, packet.payload, packet.sequence)
    }

    // ========== 解码 ==========

    fun decode(data: ByteArray): GaiaPacket? {
        if (data.size < 8) return null
        if (data[0].toInt() and 0xFF != 0xFF || data[1].toInt() and 0xFF != 0x04) return null

        val vendorId = data[5].toInt() and 0xFF
        if (vendorId != GaiaConstants.VENDOR_ID) return null

        val featureId = data[6].toInt() and 0xFF
        val commandId = data[7].toInt() and 0xFF
        val payload = if (data.size > 8) data.copyOfRange(8, data.size) else byteArrayOf()

        return GaiaPacket(featureId = featureId, commandId = commandId, payload = payload, sequence = data[4])
    }

    fun isValidPacket(data: ByteArray): Boolean {
        if (data.size < 8) return false
        if (data[0].toInt() and 0xFF != 0xFF || data[1].toInt() and 0xFF != 0x04) return false
        return (data[5].toInt() and 0xFF) == GaiaConstants.VENDOR_ID
    }

    fun isError(packet: GaiaPacket): Boolean {
        return packet.payload.isNotEmpty() && packet.payload[0] == 0xFE.toByte()
    }

    // ========== 流式解码 ==========

    class StreamDecoder {
        private var buffer = ByteArray(1024)
        private var head = 0
        private var tail = 0

        fun feed(data: ByteArray): List<GaiaPacket> {
            if (tail + data.size > buffer.size) {
                val newBuffer = ByteArray(maxOf(buffer.size * 2, tail + data.size))
                System.arraycopy(buffer, head, newBuffer, 0, tail - head)
                tail -= head
                head = 0
                buffer = newBuffer
            }
            System.arraycopy(data, 0, buffer, tail, data.size)
            tail += data.size
            return drain()
        }

        private fun drain(): List<GaiaPacket> {
            val packets = mutableListOf<GaiaPacket>()
            while (tail - head >= 8) {
                if (buffer[head].toInt() and 0xFF != 0xFF || buffer[head + 1].toInt() and 0xFF != 0x04) {
                    head++
                    continue
                }
                val vendor = buffer[head + 5].toInt() and 0xFF
                if (vendor != GaiaConstants.VENDOR_ID) {
                    head++
                    continue
                }
                val len = (buffer[head + 2].toInt() and 0xFF shl 8) or (buffer[head + 3].toInt() and 0xFF)
                val totalLen = 8 + len
                if (tail - head < totalLen) break
                val decoded = decode(buffer.copyOfRange(head, head + totalLen))
                head += totalLen
                if (decoded != null) packets.add(decoded)
            }
            if (head > 0 && tail > head) {
                val remaining = tail - head
                System.arraycopy(buffer, head, buffer, 0, remaining)
                head = 0
                tail = remaining
            } else if (head == tail) {
                head = 0; tail = 0
            }
            return packets
        }

        fun reset() { head = 0; tail = 0 }
    }
}
