package com.moondrop.protocol

import com.moondrop.protocol.codec.GaiaCodec
import com.moondrop.protocol.model.AncMode
import com.moondrop.protocol.model.GainLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GaiaCodecTest {

    @Test
    fun `encode ANC query packet`() {
        val packet = GaiaPacketBuilder.ancQuery()
        val encoded = GaiaCodec.encode(packet)

        assertEquals(0xFF.toByte(), encoded[0])
        assertEquals(0x04, encoded[1])
        // cmdId = 0x1003, little-endian: 0x03, 0x10
        assertEquals(0x03, encoded[7])
        assertEquals(0x10, encoded[8])
    }

    @Test
    fun `encode ANC set packet`() {
        val packet = GaiaPacketBuilder.ancSet(AncMode.ANC)
        val encoded = GaiaCodec.encode(packet)

        // cmdId = 0x1004, little-endian: 0x04, 0x10
        assertEquals(0x04, encoded[7])
        assertEquals(0x10, encoded[8])
        // payload: ANC mode = 0x02
        assertEquals(0x02, encoded[9])
    }

    @Test
    fun `decode ANC response`() {
        // RX format: [00] [cmdSpaceLo] [cmdSpaceHi] [cmdIdLo] [cmdIdHi] [payload...]
        val rxData = byteArrayOf(
            0x00,                   // fixed
            0x1D, 0x00,            // cmdSpace = Vendor ID (0x001D)
            0x03, 0x11,            // cmdId = 0x1103 (ANC response)
            0x02                    // payload: ANC mode = 2 (ANC)
        )

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)
        assertEquals(GaiaConstants.CMD_ANC_RESPONSE, packet.cmdId)
        assertTrue(packet.isResponse)
        assertEquals(1, packet.payload.size)
        assertEquals(AncMode.ANC, ResponseParser.parseAncMode(packet))
    }

    @Test
    fun `decode Gain response`() {
        val rxData = byteArrayOf(
            0x00,
            0x1D, 0x00,            // Vendor ID
            0x01, 0x1F,            // cmdId = 0x1F01 (Gain response)
            0x01                    // gain level = medium
        )

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)
        assertEquals(GaiaConstants.CMD_GAIN_RESPONSE, packet.cmdId)
        assertEquals(GainLevel.MEDIUM, ResponseParser.parseGainLevel(packet))
    }

    @Test
    fun `decode firmware version response`() {
        val version = "1.0.0"
        val rxData = byteArrayOf(
            0x00,
            0x1D, 0x00,            // Vendor ID
            0x05, 0x01,            // cmdId = 0x0105 (firmware)
        ) + version.toByteArray(Charsets.US_ASCII)

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)
        assertEquals(GaiaConstants.CMD_FIRMWARE_RESPONSE, packet.cmdId)

        val fwVersion = ResponseParser.parseFirmwareVersion(packet)
        assertEquals("1.0.0", fwVersion)
    }

    @Test
    fun `decode LDAC status response`() {
        val rxData = byteArrayOf(
            0x00,
            0x1D, 0x00,            // Vendor ID
            0x02, 0x21,            // cmdId = 0x2102 (LDAC response)
            0x01                    // enabled
        )

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)
        assertEquals(GaiaConstants.CMD_LDAC_RESPONSE, packet.cmdId)
        assertEquals(true, ResponseParser.parseLdacStatus(packet))
    }

    @Test
    fun `decode LC3 status response`() {
        val rxData = byteArrayOf(
            0x00,
            0x1D, 0x00,            // Vendor ID
            0x01, 0x21,            // cmdId = 0x2101 (LC3 response)
            0x00                    // disabled
        )

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)
        assertEquals(GaiaConstants.CMD_LC3_RESPONSE, packet.cmdId)
        assertEquals(false, ResponseParser.parseLc3Status(packet))
    }

    @Test
    fun `decode invalid vendor ID returns null`() {
        val rxData = byteArrayOf(
            0x00,
            0x01, 0x00,            // wrong vendor ID
            0x03, 0x10
        )

        val packet = GaiaCodec.decode(rxData)
        assertNull(packet)
    }

    @Test
    fun `decode too short data returns null`() {
        val rxData = byteArrayOf(0x00, 0x1D)
        val packet = GaiaCodec.decode(rxData)
        assertNull(packet)
    }

    @Test
    fun `anc mode from value`() {
        assertEquals(AncMode.NORMAL, AncMode.fromValue(0x01))
        assertEquals(AncMode.ANC, AncMode.fromValue(0x02))
        assertEquals(AncMode.TRANSPARENCY, AncMode.fromValue(0x04))
        assertEquals(AncMode.ADAPTIVE, AncMode.fromValue(0x08))
        assertEquals(AncMode.NORMAL, AncMode.fromValue(0xFF.toByte()))
    }

    @Test
    fun `gain level from value`() {
        assertEquals(GainLevel.HIGH, GainLevel.fromValue(0x00))
        assertEquals(GainLevel.MEDIUM, GainLevel.fromValue(0x01))
        assertEquals(GainLevel.LOW, GainLevel.fromValue(0x02))
        assertEquals(GainLevel.MEDIUM, GainLevel.fromValue(0xFF.toByte()))
    }

    @Test
    fun `isMoondropDevice`() {
        assertTrue(GaiaConstants.isMoondropDevice("MOONDROP Space Travel"))
        assertTrue(GaiaConstants.isMoondropDevice("MOONDROP Pudding"))
        assertTrue(GaiaConstants.isMoondropDevice("moondrop ultrasonic"))
    }

    @Test
    fun `response bit utilities`() {
        assertEquals(0x1103, GaiaConstants.toResponseCmdId(0x1003))
        assertTrue(GaiaConstants.isResponse(0x1103))
        assertEquals(0x1003, GaiaConstants.baseCmdId(0x1103))
    }
}
