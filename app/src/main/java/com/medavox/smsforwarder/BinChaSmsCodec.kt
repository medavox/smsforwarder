package com.medavox.smsforwarder

import android.util.Log

import java.math.BigInteger

import com.medavox.util.validate.Validator.check

/**binChaSMS encoding:
 * uses all the printable characters from SMS to represent binary data.
 *
 * TODO:
 * tests have indicated that the numbers stage is identical at the encode and decode stages.
 * This means that it's the BigInt stage that is going wrong */
/*
* WARNING:
* writing a robust, tested API for using byte arrays as arbitrary-length unsigned integers in arithmetic
* (which handles overflows in two ways: expanding the array, or silently wrapping around)
* is actually a major undertaking.
*
* Only implement the minimal subset of this functionality necessary to encode/decode:
* divide with int, returns byte[]
* multiply with int, returns byte[]
* modulo with int, returns int
* add with int(<256), returns byte[]
* */
object BinChaSmsCodec {

    private const val TAG = "BinChaSMS encoder"
    const val HEADER_SIZE = 4
    const val BYTES_PER_TEXT = 139
    const val SEPTETS_PER_TEXT = 160

    private val gsmChars = "@£$¥èéùìòÇ" +
            /*LF+*/"Øø" +/*CR+*/ "ÅåΔ_ΦΓΛΩΠΨΣΘΞ" +/*ESC+*/ "ÆæßÉ " +/*SP*/
            "!\"#¤%&'()*+,-./0123456789:; <=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿" +
            "abcdefghijklmnopqrstuvwxyzäöñüà"
    private val CHARS = gsmChars.length//should be 125
    private val CHARS_BI = BigInteger.valueOf(CHARS.toLong())
    private val charsLong = fromLong(gsmChars.length.toLong())
    private val charsAsBytes = fromLong(CHARS.toLong())

    //the results are: we can represent 139 bytes using 160 characters in an sms,
    // where we only use the printable subset of 125 values
    //System.out.println("combinations of 140 bytes:"+combinationsOf140Bytes);
    //reserve 4 bytes for header: centiseconds since sender started sending
    //(1 centisecond = 10 milliseconds)

    @Throws(IllegalArgumentException::class)
    fun encode(packet: ByteArray): String {
        check(packet.size <= BYTES_PER_TEXT - HEADER_SIZE,
                IllegalArgumentException("supplied packet was too large"))
        var out = StringBuilder()
        var input = packet
        try {
            //check(input.equalsZero(), "input was not >0")
            while (!input.equalsZero()) {//while our input-as-a-number > 0
                //built string is LSB-rightmost. no idea what endianness that corresponds to
                val indexOfChar:Int = (packet % charsAsBytes).toInt()
                System.out.println("value of char: $indexOfChar")
                check(indexOfChar < CHARS, "byte % 125 was somehow >= 125!")
                print(gsmChars[indexOfChar])
                out.append(gsmChars[indexOfChar])

                input /= charsAsBytes
            }
            //System.out.println("encoded BinCHaSMS string:"+out);
            check(out.length <= SEPTETS_PER_TEXT,
                    "byte[] packet was somehow encoded into too long a string to fit into one text!" +
                            " String length: " + out.length)
        } catch (e: Exception) {
            System.err.println("error encoding " + packet.size + "-length byte array: " + e.localizedMessage)
        }

        return out.toString()
    }

    fun decode(encoded: String): ByteArray {
        var out = newZeroedBytes(encoded.length)
        try {
            for (i in encoded.indices) {
                out *= charsAsBytes
                val numberForChar = gsmChars.indexOf(encoded[i])

                check(numberForChar in 0..(CHARS - 1),
                        "number was outside expected range. Value: $numberForChar")

                out += fromLong(numberForChar.toLong())

            }
        }catch(e: Exception) {
            System.err.println("decode failed: " + e.localizedMessage)
        }
        return out
    }
}
