package com.example.onespace.core.media

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WavFilesTest {
    /** A 44-byte-header mono 16-bit 16 kHz WAV with [samples] samples of value [v]. */
    private fun wav(dir: File, name: String, samples: Int, v: Byte): File {
        val data = ByteArray(samples * 2) { v }
        val h = ByteArray(44)
        fun put(at: Int, x: Int) { for (i in 0..3) h[at + i] = (x shr (8 * i)).toByte() }
        "RIFF".toByteArray().copyInto(h, 0); put(4, 36 + data.size)
        "WAVEfmt ".toByteArray().copyInto(h, 8); put(16, 16)
        h[20] = 1; h[22] = 1; put(24, 16_000); put(28, 32_000); h[32] = 2; h[34] = 16
        "data".toByteArray().copyInto(h, 36); put(40, data.size)
        return File(dir, name).apply { writeBytes(h + data) }
    }

    private fun int(b: ByteArray, at: Int) =
        (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or
            ((b[at + 2].toInt() and 0xFF) shl 16) or ((b[at + 3].toInt() and 0xFF) shl 24)

    @Test fun `two parts become one file with the summed length`() {
        val dir = createTempDir()
        val out = File(dir, "out.wav")
        // 16000 samples = 1 s, 8000 samples = 0.5 s
        val ms = WavFiles.concat(listOf(wav(dir, "a.wav", 16_000, 1), wav(dir, "b.wav", 8_000, 2)), out)
        assertEquals(1500L, ms)
        val bytes = out.readBytes()
        assertEquals(44 + 48_000, bytes.size)
        assertEquals(48_000, int(bytes, 40))
        assertEquals(bytes.size - 8, int(bytes, 4))
        assertEquals(1, bytes[44].toInt()); assertEquals(2, bytes[44 + 32_000].toInt())
    }

    @Test fun `no parts gives nothing`() {
        assertNull(WavFiles.concat(emptyList(), File(createTempDir(), "x.wav")))
    }

    @Test fun `a file that is not a wav is rejected`() {
        val dir = createTempDir()
        val bad = File(dir, "bad.wav").apply { writeBytes(ByteArray(10)) }
        assertNull(WavFiles.concat(listOf(bad), File(dir, "o.wav")))
        assertNotNull(wav(dir, "ok.wav", 10, 0))
    }
}
