package com.medavox.smsforwarder

import com.medavox.util.io.Bytes
import unsigned.toUbyte
import java.lang.Math.abs
import java.lang.Math.pow
import kotlin.math.pow

/**
 * @author Adam Howard

Allows you to perform basic arithmetic operations on arbitrary-length
 * byte arrays as if they were unsigned integers.
 * I am no mathematician, so there may be faster implementations.
 * BigInteger does not fit my needs.
 * NOTE: in all these methods,
 * the leftmost bit of the leftmost byte (bit 0 of byte 0) is the LEAST SIGNIFICANT.
 * Although in my mind this reads the number backwards,
 * it does mean we don't have to shift everything down by one when we add another byte,
 * and the byte indexes can be used easily as powers of 256 to make unit markers.
 *
 * this version makes use of the kotlin-unsigned library.
 * created on 12/05/2018
 */

/**Decrement the passed byte[]-uint. NOTE:
 * this method does not underflow; a call to decrement on an array of all 0s
 * merely returns the original array*/
operator fun ByteArray.dec(): ByteArray  {
    if(this.equalsZero()) {
        //if the array is all zeroes, do nothing
        return this
    }
    val ret = this.copyOf()
    for(i in 0 until ret.size) {
        val ub = ret[i].toUbyte()

        if(ub > 0) {
            //System.out.println("ubyte $i \"$ub\" is nonzero")
            ret[i] = (ub-1).toByte()
            return ret
        }
        else {//byte is 0; we need to set this byte to 255,
            // then move to the next byte to decrement there
            //System.out.println("byte $i is zero; new value: ${255.toUbyte()}")
            //System.out.println("ubyte \"$ub\" ($i of ${ret.size}) is not > 0")
            ret[i] = 255.toUbyte().toByte()
            //System.out.println("new value: ${ret[i].toUbyte()}; > 0: ${ret[i].toUbyte() > 0}")
            //because of the above guard clause,
            //we know that at least one byte in this array is non-zero
            //so we can just keep setting 0 bytes to 255
            // and moving to the next, more significant byte in the next loop,
            //until we find it
        }
    }
    return ret
}

/**Increment the passed byte[]-uint. NOTE:
 if all the bytes in the array are full, the array grows.*/
operator fun ByteArray.inc(): ByteArray {
    var ret = this.copyOf()
    for(i in 0 until ret.size) {
        val ub = ret[i].toUbyte()
        if(ub < 255) {
            ret[i] = (ub+1).toByte()
            return ret
        }
        else {//byte is 255
            ret[i] = 0
            if(i >= ret.size-1) {//array is too small to fit a new byte
                ret = ret.copyOf(ret.size+1)//grow the array
                ret[i+1] = 1//the new byte is 0, so no need to check: carry the 1!
                return ret
            }
            //else: array is already large enough for the carry,
            //but we also might encounter ANOTHER byte == 255
            //deal with that in the next loop
        }
    }
    return ret
}

/**Adds a and b, and returns an array of the same size of a.
 * The returned array grows large enough to contain the result.*/
fun ByteArray.oldplus(b2: ByteArray): ByteArray {
    var toFill:ByteArray
    var toEmpty:ByteArray
    //move 1s from smaller array into larger array
    if(this.size > b2.size) {
        toFill = this.copyOf()
        toEmpty = b2.copyOf()
    }else{
        toFill = b2.copyOf()
        toEmpty = this.copyOf()
    }
    while (!toEmpty.equalsZero()) {
        toFill++
        toEmpty--
    }
    return toFill
}


/**Adds a and b, and returns an array of the same size of a.
 * The returned array grows large enough to contain the result.
 * this implementation is faster than the old one.*/
operator fun ByteArray.plus(b2: ByteArray): ByteArray {
    var toFill:ByteArray
    val toEmpty:ByteArray
    //move 1s from smaller array into larger array
    if(this.size > b2.size) {
        toFill = this.copyOf()
        toEmpty = b2.copyOf()
    }else{
        toFill = b2.copyOf()
        toEmpty = this.copyOf()
    }
    var carryLast = false
    for (i in toEmpty.indices) {
        val carry = if(carryLast) 1 else 0
        val sum = toFill[i] + toEmpty[i] + carry
        if(sum > 255) {
            carryLast = true
            toFill[i] = (sum % 256).toByte()
        }else {
            carryLast = false
            toFill[i] = sum.toByte()
        }
    }
    if(carryLast) {//if the last element had a carry, create a new one
        toFill = toFill.copyOf(toFill.size+1)
        toFill[toFill.size-1] = 1
    }
    return toFill
}

/**Subtracts b from a, and returns an array the size of a.
 * if b > a, returns 0.*/
operator fun ByteArray.minus(byteArray: ByteArray): ByteArray {
    var a = this.copyOf()
    var b = byteArray.copyOf()
    while (!b.equalsZero() && !a.equalsZero()) {
        --a
        --b
        //System.err.println("a: ${Bytes.bytesToHex(a)}; b2: ${Bytes.bytesToHex(b)}")
        /*if(b.equalsZero()) {
            System.out.println("b equals zero; a:${Bytes.bytesToHex(a)}")
            break
        }*/
    }
    return a
}

operator fun ByteArray.times(b: ByteArray): ByteArray{
    var a = this.copyOf()
    var b2 = b.copyOf()
    while (!b2.equalsZero()) {
        a += a
        b2--
    }
    return a
}

/**return the result of a/b */
operator fun ByteArray.div(b: ByteArray): ByteArray {
    if (b > this) return newZeroedBytes()
    var a = this.copyOf()
    var result = newZeroedBytes(this.size)
    while (a > b) {
        result += 1
        a -= b
    }
    return result
}

/**return the result of a % b */
operator fun ByteArray.rem(b: ByteArray): ByteArray {
    var a = this.copyOf()
    while (a >= b) {
        a -= b
    }
    return a
}

operator fun ByteArray.compareTo(b: ByteArray): Int {
    if(this.size > b.size) {//this is larger; checks its higher elements for any nonzero bytes
        for (i in this.size-1 downTo b.size) {
            if (this[i] != 0.toByte()) {
                return 1
            }
        }
    }
    if(b.size > this.size) {//b is larger, do the same for b
        for (i in b.size-1 downTo this.size) {
            if (b[i] != 0.toByte()) {
                return -1
            }
        }
    }
    //else the arrays are of equal size, or the larger array contains only 0s in its higher bytes
    for (i in this.size downTo 0) {
        if(this[i] != b[i]) {
            return this[i].compareTo(b[i])
        }
    }
    //everything is equal
    return 0
}

fun ByteArray.equalsZero(): Boolean {
    for (b in this) {
        if (b.toInt() != 0) {
            return false
        }
    }
    return true
}

fun newZeroedBytes(length: Int=1): ByteArray {
    val result = ByteArray(length)
    for (i in result.indices) {

        result[i] = 0x00.toByte()
    }
    return result
}


@Throws(NumberFormatException::class)
fun ByteArray.toInt(): Int {
    var out:Int = 0
    for(i in this.indices) {
        val multiplier = 256.pow(i)
        val addition = this[i] * multiplier
        if((addition.toLong() + out.toLong()) > Int.MAX_VALUE ) {
            throw NumberFormatException("value of ByteArray is too high to store in an int")
        }
        out += addition
    }
    return out
}

@Throws(NumberFormatException::class)
fun ByteArray.toIntWithOverflow(): Int {
    var out:Int = 0
    for(i in this.indices) {
        val multiplier = 256.pow(i)
        out += this[i] * multiplier
    }
    return out
}

@Throws(NumberFormatException::class)
fun ByteArray.toLong(): Long {
    var out: Long = 0
    var a = this.copyOf()
    while (!a.equalsZero()) {
        if (out == java.lang.Long.MAX_VALUE) {
            throw NumberFormatException("value of argument byte[] is too high to store in a long")
        }
        a--
        out++
    }
    return out
}

@Throws(NumberFormatException::class)
fun fromLong(input: Long): ByteArray {
    var l = input
    if (l < 0) {
        throw NumberFormatException("Argument cannot be < 0. This data type is unsigned!")
    }
    var out = newZeroedBytes(8)
    while (l > 0) {
        out++
        l--
    }
    return out
}

private fun Int.pow(exp:Int): Int {
    if(exp == 0) {
        return this
    }
    val times = abs(exp)
    var subj = this
    repeat(times, {subj *= this})
    return if(exp < 0) -subj else subj
}