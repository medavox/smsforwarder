package com.medavox.smsforwarder;

import junit.framework.Assert;

import org.junit.Test;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Arrays;

import static com.medavox.smsforwarder.BinChasmsCodec.*;
import com.medavox.util.io.Bytes;
import com.medavox.util.io.BytesAsUInt;

import static com.medavox.util.io.Bytes.bytesToHex;

import static com.medavox.util.validate.Validator.check;

/**@author Adam Howard
 * @date 21/05/2017 */
public class BinChasmsCodecTests {
    private static PrintStream o = System.out;

    @Test
    public void testEncodeDecode() {
        byte[] data = generateMockPackets(BYTES_PER_TEXT-HEADER_SIZE);
        o.println("testData:      "+bytesToHex(data));
        o.println("length:"+data.length);

        String encoded = encode2(data);
        //check that every character in encoded string is one of gsmChars

        System.out.println("encoded:"+encoded);
        byte[] decoded = decode2(encoded);
        o.println("decoded byte[]:"+bytesToHex(decoded));
        o.println("length:"+decoded.length);

        /*o.println("first input   byte:"+printByteAsBinary(data[0]));
        o.println("last  input   byte:"+printByteAsBinary(data[data.length-1]));
        o.println("first decoded byte:"+printByteAsBinary(decoded[0]));
        o.println("last  decoded byte:"+printByteAsBinary(decoded[decoded.length-1]));*/
        Assert.assertTrue("input bytes didn't match decoded output!", Arrays.equals(data, decoded));
    }

    private static String printByteAsBinary(byte b) {
        String o = "";
        for(int i = 0; i <= 7; i++) {
            o += (Bytes.testBit(i, b) ? "1" : "0");
        }
        return o;
    }

    private static byte[] generateMockPackets(int lengthOfArray) {

        byte[] output = new byte[lengthOfArray];
        for(int i = 0; i < output.length; i ++) {
            output[i] = (byte)(i % 256);
        }
        return output;
    }

    /*public byte[][] generateAllPossibleByteArraysOfLength(int numBytes) {
        int combinations = (int)Math.pow(256, numBytes);
        byte[][] out = new byte[combinations][];
        for(int i = 0; i < out.length; i++) {
            out[i] = new byte[numBytes];

        }
    }*/


    @Test
    public void testBigInts_longerArrays() {
        //generate byte arrays
        byte[] b = generateMockPackets(256);
        System.out.println("BigInteger byte conversion testing");
        for(int i = 0; i < 256; i++) {
            byte[] single = new byte[]{b[i]};
            BigInteger bi = new BigInteger(single);
            byte[] back = bi.toByteArray();
            if(true) {
                //if(!Arrays.equals(single, back)) {
                System.out.println("input: "+bytesToHex(single)+" output: "+bytesToHex(back)+
                        "as number: "+bi);
            }
            //Assert.assertTrue("byte[] "+bytesToHex(single)+" and "+bytesToHex(back)+" aren't equal",
        }
    }
    @Test
    public void testBigInts() {
        byte[] b = generateMockPackets(256);
        System.out.println("BigInteger byte conversion testing");
        for(int i = 0; i < 256; i++) {
            byte[] single = new byte[]{b[i]};
            BigInteger bi = new BigInteger(single);
            byte[] back = bi.toByteArray();
            if(true) {
            //if(!Arrays.equals(single, back)) {
                System.out.println("input: "+bytesToHex(single)+" output: "+bytesToHex(back)+
                "as number: "+bi);
            }
            //Assert.assertTrue("byte[] "+bytesToHex(single)+" and "+bytesToHex(back)+" aren't equal",
        }
    }
            //        Arrays.equals(single, back));

}
