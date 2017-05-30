package com.medavox.smsforwarder;

import com.medavox.util.io.Bytes;

/**Allows you to perform basic arithmetic operations on arbitrary-length
 * byte arrays as if they were unsigned integers.
 * I am no mathematician, so there may be faster implementations.
 * BigInteger does not fit my needs.
 * NOTE: in all these methods,
 * the leftmost bit of the leftmost byte (bit 0 of byte 0) is the LEAST SIGNIFICANT.
 * Although in my mind this reads the number backwards,
 * it does mean we don't have to shift everything down by one when we add another byte,
 * and the byte indexes can be used easily as powers of 256 to make unit markers.
 *
 * ALSO, this class does nothing to guard against null bytes. So be careful.
 * todo: improve class performance*/
public abstract class BytesAsUint {
    /**Adds a and b, and returns an array of the same size of a.
     * Overflows 0 or more times.*/
    public static byte[] add(byte[] a, byte[] b) {
        //byte[] result = new byte[Math.max(a.length, b.length)];
        //per-byte outer loop
        if(a.length < b.length) {
            //if b is longer than a, swap them
            byte[] tmp = a;
            a = b;
            b = tmp;
        }
        for(int i = 0; i < b.length; i++) {
            if(b[i] != (byte)0) {
                int carry = b[i] & 0xFF;
                int j = i;
                while (carry != 0) {
                    //zip together the 2 bytes, and pass their carry to the next loop
                    if(j >= a.length) {
                        //grow array
                    }
                    int aByte = a[j] & 0xFF;
                    int result = aByte+carry;
                    a[j] = (byte)(result % 256);
                    carry = Math.max(0, result - 256);
                    j++;
                }
            }//else byte is 0, so nothing need be done
        }
        while(!equalsZero(b)) {
            increment(a);
            decrement(b);
        }
        return a;
    }

    /**Subtracts b from a, and returns an array the size of a.
     * Underflows 0 or more times.*/
    public static byte[] subtract(byte[] a, byte[] b) {
        while(!equalsZero(b)) {
            decrement(a);
            decrement(b);
        }
        return a;
    }

    public static byte[] multiply(byte[] a, byte[] b) {
        while(!equalsZero(b)) {
            add(a, a);
            decrement(b);
        }
        return a;
    }
    /**return the result of a/b*/
    public static byte[] divide(byte[] a, byte[] b) {
        byte[] result = newZeroedBytes(a.length);
        while(greaterThan(a, b)) {
            result = increment(result);
            subtract(a, b);
        }
        return result;
    }
    /**return the result of a % b*/
    public static byte[] mod(byte[] a, byte[] b) {
        byte[] result = newZeroedBytes(a.length);
        while(greaterThan(a, b)) {
            result = increment(result);
            subtract(a, b);
        }
        return a;
    }
    /**Decrement the passed byte[]-uint. NOTE:
     * just like the rest of java,
     * this method does nothing to guard against underflows!*/
    public static byte[] decrement(byte[] a) throws IllegalArgumentException {
        //moving up from LSB, set all F bits to T until we find a T bit
        //then we set that to F, then return the resulting array
        for(int i = 0; i < a.length * 8; i++) {
            if(Bytes.testBit(i % 8, a[i/8])) {
                //we've discovered a bit which is 1
                //set it to zero, then exit
                byte newByte = Bytes.unsetBit(i % 8, a[i/8]);
                a[i/8] = newByte;
                break;
            }
            else {//the first (or another) zero bit
                //set it to 1, then keep moving up
                byte newByte = Bytes.setBit(i % 8, a[i/8]);
                a[i/8] = newByte;
            }
        }
        return a;
    }
    /**Increment the passed byte[]-uint. NOTE:
     * just like the rest of java,
     * this method does nothing to guard against overflows!*/
    public static byte[] increment(byte[] a) {
        //moving up from LSB, set all T bits to F until we find a F bit
        //then we set that to T, then return the resulting array
        for(int i = 0; i < a.length * 8; i++) {
            if(Bytes.testBit(i % 8, a[i/8])) {
                //we've discovered a bit which is 1
                //set it to zero, then move up
                byte newByte = Bytes.unsetBit(i % 8, a[i/8]);
                a[i/8] = newByte;
            }
            else {//the first (or another) zero bit
                //set it to 1, then exit
                byte newByte = Bytes.setBit(i % 8, a[i/8]);
                a[i/8] = newByte;
                break;
            }
        }
        return a;
    }

    /**Returns true if a < b.*/
    public static boolean lessThan(byte[] a, byte[] b) {
        if(a.length != b.length) {
            return a.length < b.length;
        }
        else {
            for (int i = (a.length*8)-1; i > 0; i--) {
                boolean bitA = Bytes.testBit(i % 8, a[i/8]);
                boolean bitB = Bytes.testBit(i % 8, b[i/8]);

                if(bitA != bitB) {
                    //if they're not equal, then one is true and the other false
                    //if bitB is the true one, A is lesser, so return true
                    //if bitB is the false one, A is greater, so return false
                    return bitB;
                }
            }
            //everything is equal
            return false;
        }
    }

    /**Returns true if a > b.*/
    public static boolean greaterThan(byte[] a, byte[] b) {
        for (int i = (a.length *8)-1; i > 0; i--) {
            boolean bitA = Bytes.testBit(i % 8, a[i/8]);
            boolean bitB = Bytes.testBit(i % 8, b[i/8]);

            if(bitA != bitB) {
                //if they're not equal, then one is true and the other false
                //if bitA is the false one, it's lesser, so return false
                //if bitA is the true one, it is greater, so return true
                return bitA;
            }
        }
        //everything is equal
        return false;
    }

    /**Checks whether every byte in the passed array is equal to 0x00.*/
    public static boolean equalsZero(byte[] a) {
        for(byte b : a) {
            if(b  != 0) {
                return false;
            }
        }
        return true;
    }

    public static byte[] newZeroedBytes(int length) {
        byte[] result = new byte[length];
        for(byte b : result) {
            b = (byte)0x00;
        }
        return result;
    }

    public static int toInt (byte[] a) throws NumberFormatException {
        if(a.length > 4) {
            //the number is too large to store in a long
            throw new NumberFormatException("byte[] is too large to store in an int");
        }
        int out = 0;
        while(!equalsZero(a)) {
            if(out == Integer.MAX_VALUE) {
                throw new NumberFormatException("value of argument byte[] is too high to store in an int");
            }
            decrement(a);
            out++;
        }
        return out;
    }

    public static long toLong (byte[] a) throws NumberFormatException {
        if(a.length > 8) {
            //the number is too large to store in a long
            throw new NumberFormatException("byte[] is too long to store in a long");
        }
        long out = 0;
        while(!equalsZero(a)) {
            if(out == Long.MAX_VALUE) {
                throw new NumberFormatException("value of argument byte[] is too high to store in a long");
            }
            decrement(a);
            out++;
            //todo:handle overflows, from our 64 bits being too large for long's 2^63-1
        }
        return out;
    }

    public static byte[] fromLong(long l) throws NumberFormatException {
        if(l < 0) {
            throw new NumberFormatException("Argument cannot be < 0. This data type is unsigned!");
        }
        byte[] out = newZeroedBytes(8);
        while(l > 0) {
            increment(out);
            l--;
        }
        return newZeroedBytes(1);
    }
}
