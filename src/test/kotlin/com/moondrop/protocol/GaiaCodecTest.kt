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

    @Test
    fun `encode battery query packet`() {
        val packet = GaiaPacketBuilder.batteryQuery()
        val encoded = GaiaCodec.encode(packet)

        assertEquals(0xFF.toByte(), encoded[0])
        assertEquals(0x04, encoded[1])
        assertEquals(GaiaConstants.FEATURE_DEVICE_MANAGEMENT, encoded[8])
        assertEquals(GaiaConstants.CMD_BATTERY, encoded[9])
        assertEquals(0x01, encoded[10]) // payload[0]
        assertEquals(0x02, encoded[11]) // payload[1]
    }

    @Test
    fun `encode ANC set packet`() {
        val packet = GaiaPacketBuilder.ancSet(AncMode.ANC)
        val encoded = GaiaCodec.encode(packet)

        assertEquals(GaiaConstants.FEATURE_ANC_V2, encoded[8])
        assertEquals(GaiaConstants.CMD_ANC_SET_MODE, encoded[9])
        assertEquals(AncMode.ANC.value, encoded[10])
    }

    @Test
    fun `decode RX packet`() {
        val rxData = byteArrayOf(
            0x00, 0x1D, // vendor ID = 29
            0x03,       // feature ID = ANC
            0x83,       // cmd ID = 0x03 | 0x80 (response)
            0x02        // payload: ANC mode = 2 (ANC)
        )

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)
        assertEquals(GaiaConstants.FEATURE_ANC_V2, packet.featureId)
        assertEquals(0x83.toByte(), packet.cmdId)
        assertTrue(packet.isResponse)
        assertEquals(1, packet.payload.size)
        assertEquals(AncMode.ANC, ResponseParser.parseAncMode(packet))
    }

    @Test
    fun `decode battery response`() {
        val rxData = byteArrayOf(
            0x00, 0x1D, // vendor ID
            0x00,       // feature ID = device management
            0x00,       // cmd ID
            0x64,       // left battery = 100%
            0x50        // right battery = 80%
        )

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)

        val battery = ResponseParser.parseBattery(packet)
        assertNotNull(battery)
        assertEquals(100, battery.first)
        assertEquals(80, battery.second)
    }

    @Test
    fun `decode firmware version response`() {
        val version = "1.0.0"
        val rxData = byteArrayOf(
            0x00, 0x1D, // vendor ID
            0x01,       // feature ID = basic function
            0x05,       // cmd ID = firmware version
        ) + version.toByteArray(Charsets.US_ASCII)

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)

        val fwVersion = ResponseParser.parseFirmwareVersion(packet)
        assertEquals("1.0.0", fwVersion)
    }

    @Test
    fun `decode gain response`() {
        val rxData = byteArrayOf(
            0x00, 0x1D, // vendor ID
            0x1E,       // feature ID = gain
            0x01,       // cmd ID = get
            0x01        // gain level = medium
        )

        val packet = GaiaCodec.decode(rxData)
        assertNotNull(packet)

        val gain = ResponseParser.parseGainLevel(packet)
        assertEquals(GainLevel.MEDIUM, gain)
    }

    @Test
    fun `decode invalid vendor ID returns null`() {
        val rxData = byteArrayOf(
            0x00, 0x01, // wrong vendor ID
            0x03, 0x04
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
        assertEquals(AncMode.NORMAL, AncMode.fromValue(0xFF.toByte())) // unknown → NORMAL
    }

    @Test
    fun `gain level from value`() {
        assertEquals(GainLevel.HIGH, GainLevel.fromValue(0x00))
        assertEquals(GainLevel.MEDIUM, GainLevel.fromValue(0x01))
        assertEquals(GainLevel.LOW, GainLevel.fromValue(0x02))
        assertEquals(GainLevel.MEDIUM, GainLevel.fromValue(0xFF.toByte())) // unknown → MEDIUM
    }

    @Test
    fun `isMoondropDevice`() {
        assertTrue(GaiaConstants.isMoondropDevice("MOONDROP Space Travel"))
        assertTrue(GaiaConstants.isMoondropDevice("MOONDROP Pudding"))
        assertTrue(GaiaConstants.isMoondropDevice("moondrop ultrasonic"))
    }
}
