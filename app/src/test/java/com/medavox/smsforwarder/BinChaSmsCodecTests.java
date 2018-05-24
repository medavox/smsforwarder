package com.medavox.smsforwarder;

import junit.framework.Assert;

import org.junit.Test;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Arrays;

import static com.medavox.smsforwarder.BinChaSmsCodec.*;
import com.medavox.util.io.Bytes;

import static com.medavox.smsforwarder.ByteArraysAsUIntKt.fromLong;
import static com.medavox.util.io.Bytes.bytesToHex;

import static com.medavox.util.validate.Validator.check;

/**@author Adam Howard
 * @date 21/05/2017 */
public class BinChaSmsCodecTests {
    private static PrintStream o = System.out;
    private final byte[][] inputs = new byte[][]{
            new byte[]{(byte)255, 0x01, 0x02},
            new byte[]{0x0, 0x01, 0x02},
            new byte[]{0x0, 0x00, 0x02},
            new byte[]{0x0, 0x00, 0x00}
    };

    @Test
    public void testEncodeDecode() {
        byte[] data = generateMockPackets(BYTES_PER_TEXT - HEADER_SIZE);
        o.println("testData:      "+bytesToHex(data));
        o.println("length:"+data.length);

        String encoded = INSTANCE.encode(data);
        //check that every character in encoded string is one of gsmChars

        System.out.println("encoded:"+encoded);
        byte[] decoded = INSTANCE.decode(encoded);
        o.println("decoded byte[]:"+bytesToHex(decoded));
        o.println("length:"+decoded.length);

        /*o.println("first input   byte:"+printByteAsBinary(data[0]));
        o.println("last  input   byte:"+printByteAsBinary(data[data.length-1]));
        o.println("first decoded byte:"+printByteAsBinary(decoded[0]));
        o.println("last  decoded byte:"+printByteAsBinary(decoded[decoded.length-1]));*/
        Assert.assertTrue("input bytes didn't match decoded output!", Arrays.equals(data, decoded));
    }

    @Test
    public void moduloTest() {
        long input = 8543798254L;
        long arg = 7;

        System.out.println("input:"+input+"; arg:"+arg);
        byte[] asBytes = fromLong(input);
        byte[] argBytes = fromLong(arg);
        System.out.println("as bytes:"+Bytes.bytesToHex(asBytes)+", "+Bytes.bytesToHex(argBytes));
        System.out.println("result: "+Bytes.bytesToHex(ByteArraysAsUIntKt.div(asBytes, argBytes)));
    }

    private interface TestNoArgs {
        byte[] run(byte[] input);
    }

    private interface TestOneArg {
        byte[] run(byte[] input, byte[] arg);
    }

    private void noArgTest(byte[] input, byte[] expectedResult, TestNoArgs tester) {
        System.out.println();
        System.out.println("input          : "+bytesToHex(input));
        System.out.println("expected result: "+bytesToHex(expectedResult));
        byte[] actualResult = tester.run(input);
        //PrintStream p = (Arrays.equals(expectedResult, actualResult) ? System.out : System.err);
        System.out.println("actual   result: "+bytesToHex(actualResult));
        Assert.assertTrue(Arrays.equals(expectedResult, actualResult));
    }

    private void oneArgTest(byte[] input, byte[] arg, byte[] expectedResult, TestOneArg tester) {
        System.out.println();
        System.out.println("input1 (subj)  : "+bytesToHex(input));
        System.out.println("input2 (arg)   : "+bytesToHex(input));
        System.out.println("expected result: "+bytesToHex(expectedResult));
        byte[] actualResult = tester.run(input, arg);
        //PrintStream p = (Arrays.equals(expectedResult, actualResult) ? System.out : System.err);
        System.out.println("actual   result: "+bytesToHex(actualResult));
        Assert.assertTrue(Arrays.equals(expectedResult, actualResult));
    }

    @Test
    public void additionTest() {
        byte[] input1 = new byte[]{0, 1, 2, 3};
        byte[] input2 = new byte[]{0, 1, 2, 3};
        byte[] expectedResult = new byte[]{0, 2, 4, 6};

        oneArgTest(input1, input2, expectedResult,
            new TestOneArg() {
                @Override public byte[] run(byte[] input, byte[] arg) {
                    return ByteArraysAsUIntKt.plus(input, arg);
                }
            }
        );
    }

    @Test
    public void incrementTest() {
        byte[][] expectedIncResults = new byte[][]{
                new byte[]{0x00, 0x02, 0x02},
                new byte[]{0x01, 0x01, 0x02},
                new byte[]{0x01, 0x00, 0x02},
                new byte[]{0x01, 0x00, 0x00}
        };
        for(int i = 0; i < inputs.length; i++) {
            noArgTest(inputs[i], expectedIncResults[i], new TestNoArgs() {
                @Override public byte[] run(byte[] input) {
                    return ByteArraysAsUIntKt.inc(input);
                }
            });
        }
    }

    @Test
    public void decrementTest() {
        //byte[] expectedResult = new byte[]{(byte)0xFF, 0x00, 0x02};
        //byte[] testInput = new byte[]{0x0, 0x01, 0x02};
        byte[][] expectedDecResults = new byte[][]{
                new byte[]{(byte)254, 0x01, 0x02},
                new byte[]{(byte)255, 0x00, 0x02},
                new byte[]{(byte)255, (byte)255, 0x01},
                new byte[]{0x00, 0x00, 0x00}
        };
        for(int i = 0; i < inputs.length; i++) {
            noArgTest(inputs[i], expectedDecResults[i], new TestNoArgs() {
                @Override public byte[] run(byte[] input) {
                    return ByteArraysAsUIntKt.dec(input);
                }
            });
        }
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
