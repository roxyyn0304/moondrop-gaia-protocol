package com.moondrop.protocol

import com.moondrop.protocol.codec.GaiaCodec
import com.moondrop.protocol.model.AncMode
import com.moondrop.protocol.model.GainLevel
import com.moondrop.protocol.model.GaiaPacket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GaiaCodecTest {

    private fun hex(vararg bytes: Int): ByteArray = ByteArray(bytes.size) { bytes[it].toByte() }

    // ========== 编码 ==========

    @Test
    fun `encode firmware version query`() {
        val encoded = GaiaCodec.encode(GaiaPacketBuilder.firmwareVersionQuery())
        assertEquals(0xFF, encoded[0].toInt() and 0xFF)
        assertEquals(0x04, encoded[1].toInt() and 0xFF)
        assertEquals(0x00, encoded[6].toInt() and 0xFF) // FEATURE_BASE
        assertEquals(0x05, encoded[7].toInt() and 0xFF) // CMD_FIRMWARE_VERSION
    }

    @Test
    fun `encode ANC query`() {
        val encoded = GaiaCodec.encode(GaiaPacketBuilder.ancQuery())
        assertEquals(0x40, encoded[6].toInt() and 0xFF) // FEATURE_ANC
        assertEquals(0x03, encoded[7].toInt() and 0xFF) // CMD_ANC_QUERY
    }

    @Test
    fun `encode ANC set`() {
        val encoded = GaiaCodec.encode(GaiaPacketBuilder.ancSet(AncMode.ADAPTIVE))
        assertEquals(0x40, encoded[6].toInt() and 0xFF)
        assertEquals(0x04, encoded[7].toInt() and 0xFF)
        assertEquals(0x01, encoded[8].toInt() and 0xFF) // ANC mode = 0x01
    }

    @Test
    fun `encode len is payload size`() {
        val encoded = GaiaCodec.encode(GaiaPacket(0x40, 0x04, byteArrayOf(0x01, 0x02, 0x03)))
        assertEquals(0, encoded[2].toInt() and 0xFF, "len high byte")
        assertEquals(3, encoded[3].toInt() and 0xFF, "len low byte")
        assertEquals(11, encoded.size, "total size = 8 + 3")
    }

    // ========== 解码 ==========

    @Test
    fun `decode firmware version`() {
        val rx = hex(0xFF, 0x04, 0x00, 0x05, 0x00, 0x1D, 0x01, 0x05) +
                "3.5.2".toByteArray(Charsets.US_ASCII)
        val p = GaiaCodec.decode(rx)
        assertNotNull(p)
        assertEquals(0x01, p.featureId) // response bit set
        assertEquals(0x05, p.commandId)
        assertEquals("3.5.2", ResponseParser.parseFirmwareVersion(p))
    }

    @Test
    fun `decode ANC status`() {
        val rx = hex(0xFF, 0x04, 0x00, 0x01, 0x00, 0x1D, 0x41, 0x03, 0x02)
        val p = GaiaCodec.decode(rx)
        assertNotNull(p)
        assertEquals(0x41, p.featureId) // 0x40 | 0x01 response bit
        assertEquals(AncMode.TRANSPARENCY, ResponseParser.parseAncMode(p))
    }

    @Test
    fun `decode ANC off`() {
        val rx = hex(0xFF, 0x04, 0x00, 0x01, 0x00, 0x1D, 0x41, 0x03, 0x00)
        val p = GaiaCodec.decode(rx)
        assertNotNull(p)
        assertEquals(AncMode.OFF, ResponseParser.parseAncMode(p))
    }

    @Test
    fun `decode ANC anti-wind`() {
        val rx = hex(0xFF, 0x04, 0x00, 0x01, 0x00, 0x1D, 0x41, 0x03, 0x03)
        val p = GaiaCodec.decode(rx)
        assertNotNull(p)
        assertEquals(AncMode.ANTI_WIND, ResponseParser.parseAncMode(p))
    }

    @Test
    fun `decode ANC adaptive`() {
        val rx = hex(0xFF, 0x04, 0x00, 0x01, 0x00, 0x1D, 0x41, 0x03, 0x01)
        val p = GaiaCodec.decode(rx)
        assertNotNull(p)
        assertEquals(AncMode.ADAPTIVE, ResponseParser.parseAncMode(p))
    }

    @Test
    fun `decode invalid header`() {
        assertNull(GaiaCodec.decode(hex(0x00, 0x04, 0x00, 0x00, 0x00, 0x1D, 0x05, 0x00)))
    }

    @Test
    fun `decode wrong vendor`() {
        assertNull(GaiaCodec.decode(hex(0xFF, 0x04, 0x00, 0x00, 0x00, 0x01, 0x05, 0x00)))
    }

    @Test
    fun `decode too short`() {
        assertNull(GaiaCodec.decode(hex(0xFF, 0x04, 0x00)))
    }

    // ========== 工具方法 ==========

    @Test
    fun `isResponse and baseFeatureId`() {
        assertTrue(GaiaConstants.isResponse(0x41))
        assertEquals(0x40, GaiaConstants.baseFeatureId(0x41))
    }

    @Test
    fun `packet cmdId`() {
        assertEquals(0x0005, GaiaPacket(0x00, 0x05).cmdId)
    }

    // ========== ResponseParser ==========

    @Test
    fun `parse firmware fails on wrong feature`() {
        assertNull(ResponseParser.parseFirmwareVersion(GaiaPacket(0x07, 0x05, "1.0".toByteArray())))
    }

    @Test
    fun `parse anc mode`() {
        val p = GaiaPacket(0x41, 0x03, byteArrayOf(0x02))
        assertEquals(AncMode.TRANSPARENCY, ResponseParser.parseAncMode(p))
    }

    @Test
    fun `parse anc mode from SET response`() {
        val p = GaiaPacket(0x41, 0x04, byteArrayOf(0x03, 0x02, 0x00))
        assertEquals(AncMode.ANTI_WIND, ResponseParser.parseAncMode(p))
    }

    @Test
    fun `parse anc adaptive from SET response`() {
        val p = GaiaPacket(0x41, 0x04, byteArrayOf(0x01, 0x02, 0x00))
        assertEquals(AncMode.ADAPTIVE, ResponseParser.parseAncMode(p))
    }

    @Test
    fun `parse gain level from SET response`() {
        val p = GaiaPacket(0x1F, 0x02, byteArrayOf(0x02, 0x00, 0x00))
        assertEquals(GainLevel.LOW, ResponseParser.parseGainLevel(p))
    }

    @Test
    fun `isSuccess`() {
        assertTrue(ResponseParser.isSuccess(GaiaPacket(0, 0)))
        assertTrue(ResponseParser.isSuccess(GaiaPacket(0, 0, byteArrayOf(0x00))))
    }

    // ========== StreamDecoder ==========

    private fun testPkt(fid: Int, cid: Int, vararg payload: Int): ByteArray {
        return GaiaCodec.encode(GaiaPacket(fid, cid, ByteArray(payload.size) { payload[it].toByte() }))
    }

    @Test
    fun `stream single`() {
        assertEquals(1, GaiaCodec.StreamDecoder().feed(testPkt(0x00, 0x05, 0x33, 0x2E)).size)
    }

    @Test
    fun `stream two packets`() {
        val d = GaiaCodec.StreamDecoder()
        assertEquals(2, d.feed(testPkt(0x00, 0x05) + testPkt(0x00, 0x14, 0x41)).size)
    }

    @Test
    fun `stream split`() {
        val d = GaiaCodec.StreamDecoder()
        val full = testPkt(0x00, 0x05, 0x33)
        assertEquals(0, d.feed(full.copyOfRange(0, 5)).size)
        assertEquals(1, d.feed(full.copyOfRange(5, full.size)).size)
    }

    @Test
    fun `stream byte by byte`() {
        val d = GaiaCodec.StreamDecoder()
        val full = testPkt(0x00, 0x07)
        var count = 0
        for (b in full) count += d.feed(byteArrayOf(b)).size
        assertEquals(1, count)
    }

    @Test
    fun `stream empty`() {
        assertEquals(0, GaiaCodec.StreamDecoder().feed(byteArrayOf()).size)
    }
}
