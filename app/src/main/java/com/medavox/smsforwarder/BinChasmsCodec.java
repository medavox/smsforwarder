package com.medavox.smsforwarder;

import android.util.Log;

import java.io.PrintStream;
import java.math.BigInteger;

import com.medavox.util.io.Bytes;
import com.medavox.util.io.BytesAsUInt;
import com.medavox.util.io.StringUtils;

import static com.medavox.util.validate.Validator.check;
import static com.medavox.util.io.BytesAsUInt.*;

/**binChaSMS encoding:
 * uses all the printable characters from SMS to represent binary data.
 *
 * TODO:
 * tests have indicated that the numbers stage is identical at the encode and decode stages.
 * This means that it's the BigInt stage that is going wrong*/
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
public class BinChasmsCodec {

    private static final String TAG = "BinChaSMS encoder";
    final static int HEADER_SIZE = 4;
    final static int BYTES_PER_TEXT = 139;
    private final static int SEPTETS_PER_TEXT  = 160;

    private static final String gsmChars = "@£$¥èéùìòÇ"+
        /*LF+*/"Øø"+/*CR+*/"ÅåΔ_ΦΓΛΩΠΨΣΘΞ"+/*ESC+*/"ÆæßÉ "+/*SP*/
        "!\"#¤%&'()*+,-./0123456789:; <=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿" +
            "abcdefghijklmnopqrstuvwxyzäöñüà";
    private static int CHARS = gsmChars.length();//should be 125
    private static BigInteger CHARS_BI = BigInteger.valueOf(CHARS);
    private static byte[] charsLong = fromLong(gsmChars.length());

    //the results are: we can represent 139 bytes using 160 characters in an sms,
        // where we only use the printable subset of 125 values
    //System.out.println("combinations of 140 bytes:"+combinationsOf140Bytes);
    //reserve 4 bytes for header: centiseconds since sender started sending
    //(1 centisecond = 10 milliseconds)

    /**Adds a value to the passed byte array, as if it were an arbitrary-length unsigned integer.
     * Expands the length of the array if necessary.
     * @param b the value to add. Must be >=0.*/
    private static byte[] addy(byte[] a, int b) throws IllegalArgumentException {
        if(b < 0) { throw new IllegalArgumentException("integer must not be negative"); }
        if(b > 255) { throw new IllegalArgumentException("integer must not be >255"); }
        //per-byte outer loop
        int carry = b;
        int j = 0;
        while (carry != 0) {
            //add the byte represented by b to the LSB, carrying any extra
            if(j >= a.length-1) {
                a = growArrayByOne(a);
            }
            int aByte = a[j] & 0xFF;
            int result = aByte+carry;//the max this can be is 510
            a[j] = (byte)(result % 256);
            carry = result / 256;//max is 1
            j++;
        }
        return a;
    }

    private static byte[] growArrayByOne(byte[] a) {
        byte[] out = new byte[a.length+1];
        for(int i = 0; i < a.length; i++) {
            out[i] = a[i];
        }
        out[a.length] = (byte)0;
        return out;
    }


    /**Checks whether every byte in the passed array is equal to 0x00.*/
    private static boolean equalsZero(byte[] a) {
        for(byte b : a) {
            if(b  != 0) {
                return false;
            }
        }
        return true;
    }

    public static String encode2(byte[] packet) throws IllegalArgumentException {
        String out = "";

        while(!equalsZero(packet)) {
            int indexOfChar = toInt(mod(packet, charsLong));
            out += gsmChars.charAt(indexOfChar);
            packet = divide(packet, charsLong);
        }
        return out;
    }

    public static byte[] decode2(String enc) {
//todo:build BytesAsUint class variant where overflows cause the byte[] to grow, not wraparound
        int lengthOfBytes = Bytes.computeBytesStorable(enc.length(), gsmChars.length());
        byte[] out = newZeroedBytes(lengthOfBytes);
        try {
            //String numbers = "";
            for (int i = enc.length() - 1; i >= 0; i--) {
                out = multiply(out, charsLong);
                char c = enc.charAt(i);
                int index = gsmChars.indexOf(c);
                //number should be between 0 and 124 inclusive
                //System.out.println("value of char "+i+":"+index);
                //numbers = index+" " + numbers;
                check(index >= 0 && index < CHARS,
                        "index of character \'"+c+"\' was not between 0 and 124 inclusive. Value: " + index);
                out = add(out, fromLong(index));
            }
            //System.out.println("output numbers: "+numbers);
        }
        catch(Exception e) {
            System.err.println("decode failed: "+e.getLocalizedMessage());
        }
        return out;
    }

    public static String encode(byte[] packet) throws IllegalArgumentException {
        check(packet.length <= (BYTES_PER_TEXT - HEADER_SIZE),
                new IllegalArgumentException("supplied packet was too large"));
        String out = "";
        BigInteger asNumber = new BigInteger(/*1,guarantees positivity*/ packet);
        System.out.println("input bytes as a number:"+asNumber);

        try {
            check (asNumber.compareTo(BigInteger.ZERO) > 0, "big integer was not >0");
            String numbers = "";

            while (asNumber.compareTo(BigInteger.ZERO) > 0) {//while our input-as-a-number > 0
                //built string is LSB-rightmost. no idea what endianness that corresponds to
                BigInteger indexOfChar = asNumber.mod(CHARS_BI);
                //System.out.println("value of char "+i+":"+indexOfChar);
                numbers += " "+indexOfChar;
                check(indexOfChar.compareTo(CHARS_BI) < 0, "byte % 125 was somehow >= 125!");

                out += gsmChars.charAt(indexOfChar.intValue());

                asNumber = asNumber.divide(CHARS_BI);
            }
            System.out.println("input numbers: "+numbers);
            //System.out.println("encoded BinCHaSMS string:"+out);
            check(out.length() <= SEPTETS_PER_TEXT,
                    "byte[] packet was somehow encoded into too long a string to fit into one text!" +
                            " String length: "+out.length());
        }
         catch(Exception e) {
             Log.e(TAG, "error encoding "+packet.length+"-length byte array: "+e.getLocalizedMessage());
         }
         return out;
    }

    public static byte[] decode(String encoded) {
        BigInteger asNumber = BigInteger.ZERO;
        try {
            String numbers = "";
            for (int i = encoded.length() - 1; i >= 0; i--) {
                asNumber = asNumber.multiply(CHARS_BI);
                //String c = new String(new char[]{encoded.charAt(i)});
                char c = encoded.charAt(i);
                int index = gsmChars.indexOf(c);
                //number should be between 0 and 124 inclusive
                //System.out.println("value of char "+i+":"+index);
                numbers = index+" " + numbers;
                check(index >= 0 && index < CHARS,
                        "number was outside expected range. Value: " + index);
                asNumber = asNumber.add(BigInteger.valueOf(index));
            }
            System.out.println("output numbers: "+numbers);
        }
        catch(Exception e) {
            System.err.println("decode failed: "+e.getLocalizedMessage());
        }
        System.out.println("string into a number:   "+asNumber);
        return asNumber.toByteArray();
        //fixme:any leading zero-bytes are omitted. We can't just pad the bytes with zeroes,
        //because we don't know how long the original was (<=139 bytes)
        //byte[] bytes = asNumber.toByteArray();
        //check(bytes.length <= )
        //if(bytes.length )
    }
}
