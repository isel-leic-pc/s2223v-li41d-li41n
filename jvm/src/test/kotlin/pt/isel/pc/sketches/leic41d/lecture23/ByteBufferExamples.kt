package pt.isel.pc.sketches.leic41d.lecture23

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.CharBuffer
import kotlin.test.assertEquals

class ByteBufferExamples {

    @Test
    fun first() {
        val bb = ByteBuffer.allocate(10)
        // \x6f\x6c\xc3\xa1
        bb.put(0x6f)
        bb.put(0x6c)
        bb.put(0xc3.toByte())
        bb.put(0xa1.toByte())
        bb.flip()
        assertEquals(0, bb.position())
        assertEquals(4, bb.limit())

        assertEquals(0x6f, bb.get())
        assertEquals(0x6c, bb.get())
        assertEquals(0xc3.toByte(), bb.get())
        assertEquals(0xa1.toByte(), bb.get())
    }

    @Test
    fun `CharsetDecoder simple example`() {
        val string = "olá"
        val charset = Charsets.UTF_8
        val bytes = ByteBuffer.wrap(string.toByteArray(charset))
        val decoder = charset.newDecoder()
        val chars = CharBuffer.allocate(16)

        // bytes[0] -> chars[0]
        bytes.limit(1)
        var result = decoder.decode(bytes, chars, false)
        Assertions.assertFalse(result.isError)
        assertEquals(1, bytes.position(), "one byte is consumed")
        assertEquals(1, chars.position(), "one char is produced")
        assertEquals('o', chars.get(0))

        // bytes[1] -> chars[1]
        bytes.limit(2)
        result = decoder.decode(bytes, chars, false)
        Assertions.assertFalse(result.isError)
        assertEquals(2, bytes.position(), "one byte is consumed")
        assertEquals(2, chars.position(), "one char is produced")
        assertEquals('l', chars.get(1))

        // bytes[2] -> no byte is consumed, no char is produced
        bytes.limit(3)
        result = decoder.decode(bytes, chars, false)
        Assertions.assertFalse(result.isError)
        assertEquals(2, bytes.position(), "no byte is consumed")
        assertEquals(2, chars.position(), "no char is consumed")

        // (bytes[2], bytes[3]) -> chars[2]
        bytes.limit(4)
        result = decoder.decode(bytes, chars, false)
        Assertions.assertFalse(result.isError)
        assertEquals(4, bytes.position(), "two bytes are consumed")
        assertEquals(3, chars.position(), "one char is produced")
        assertEquals('á', chars.get(2))
    }
}