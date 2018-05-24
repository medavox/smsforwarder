package com.medavox.smsforwarder;

import com.medavox.util.io.BytesAsUInt;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;

import static com.medavox.util.io.Bytes.bytesToHex;

/**
 * @author Adam Howard
 * @date 24/05/2017
 */

public class BytesAsUIntTests {
    private static final int leng = 256 * 256;

    private byte[] genByteArray(int i) {
        byte[] b = new byte[2];
        b[0] = (byte)(i % 256);
        b[1] = (byte)(i / 256);
        return b;
    }

    public interface SingleArgumentTest {
        void test(int i);
    };

    public interface DoubleArgumentTest {
        void test(int i, int j);
    };

    public void allSingly(SingleArgumentTest test) {
        for(int i = 0; i < leng; i++) {
            test.test(i);
        }
    }

    public void allCombinations(DoubleArgumentTest turst) {
        for(int i = 0; i < leng; i++) {
            for(int j = 0; j < leng; j++) {
                turst.test(i, j);
            }
        }
    }

    public void uniquePairs(DoubleArgumentTest pairs) {
        for(int i = 0; i < leng-2; i++) {
            for(int j = i+1; j < leng-1; j++) {
                pairs.test(i, j);
            }
        }
    }

    //------------------ACTUAL TESTS---------------------------

    @Test
    public void test_increment()  {
        allSingly(new SingleArgumentTest() {
            @Override public void test(int i) {
                byte[] test = genByteArray(i);
                //byte[] manualZeroes = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                //byte[] manualZeroOne = new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                byte[] result = BytesAsUInt.increment(test);
                byte[] exp = genByteArray(i+1);
                Assert.assertTrue("expected:"+bytesToHex(exp)+"; result:"+bytesToHex(result),
                        Arrays.equals(exp, result));
            }
        });
    }

    @Test
    public void test_decrement()  {
        allSingly(new SingleArgumentTest() {
            @Override public void test(int i) {
                byte[] test = genByteArray(i);
                //byte[] manualZeroes = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                //byte[] manualZeroOne = new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                byte[] result = BytesAsUInt.decrement(test);
                byte[] exp = genByteArray(i-1);
                Assert.assertTrue("expected:"+bytesToHex(exp)+"; result:"+bytesToHex(result),
                        Arrays.equals(exp, result));
            }
        });
    }

    @Test
    public void test_add() {

        uniquePairs(new DoubleArgumentTest() {
            @Override public void test(int i, int j) {
                byte[] a = genByteArray(i);
                byte[] b = genByteArray(j);
                byte[] exp = genByteArray(i+j);
                if((j % 1024) == 0) {
                    System.out.println("i:" + i + "; j:" + j);
                }
                byte[] result = BytesAsUInt.add(a, b);
                Assert.assertTrue("expected:"+bytesToHex(exp)+"; result:"+bytesToHex(result),
                        Arrays.equals(exp, result));
            }
        });
    }

    @Test
    public void test_subtract() {
        allCombinations(new DoubleArgumentTest() {
            @Override public void test(int i, int j) {
                byte[] a = genByteArray(i);
                byte[] b = genByteArray(j);
                byte[] expected = genByteArray(Math.max(0, i-j));

                byte[] result = BytesAsUInt.subtract(a, b);
                Assert.assertTrue("expected:"+bytesToHex(expected)+"; result:"+bytesToHex(result),
                        Arrays.equals(expected, result));
            }
        });
    }

    @Test
    public void test_multiply() {
        uniquePairs(new DoubleArgumentTest() {
            @Override public void test(int i, int j) {
                byte[] a = genByteArray(i);
                byte[] b = genByteArray(j);
                byte[] exp = genByteArray(i*j);

                byte[] result = BytesAsUInt.multiply(a, b);
                Assert.assertTrue("expected:"+bytesToHex(exp)+"; result:"+bytesToHex(result),
                        Arrays.equals(exp, result));
            }
        });
    }

    @Test
    public void test_divide() {
        allCombinations(new DoubleArgumentTest() {
            @Override public void test(int i, int j) {
                byte[] a = genByteArray(i);
                byte[] b = genByteArray(j);
                byte[] exp = genByteArray(i / j);

                byte[] result = BytesAsUInt.divide(a, b);
                Assert.assertTrue("expected:" + bytesToHex(exp) + "; result:" + bytesToHex(result),
                        Arrays.equals(exp, result));
            }
        });
    }

    @Test
    public void test_mod() {
        uniquePairs(new DoubleArgumentTest() {
            @Override public void test(int i, int j) {
                byte[] a = genByteArray(i);
                byte[] b = genByteArray(j);
                byte[] exp = genByteArray(i % j);

                byte[] result = BytesAsUInt.mod(a, b);
                Assert.assertTrue("expected:"+bytesToHex(exp)+"; result:"+bytesToHex(result),
                        Arrays.equals(exp, result));
            }
        });
    }

    @Test
    public void test_greaterThan() {
        allCombinations(new DoubleArgumentTest() {
            @Override
            public void test(int i, int j) {
                byte[] a = genByteArray(i);
                byte[] b = genByteArray(j);
                boolean expected = i > j;

                boolean result = BytesAsUInt.greaterThan(a, b);

            }
        });
    }

    @Test
    public void test_lessThan() {
        allCombinations(new DoubleArgumentTest() {
            @Override
            public void test(int i, int j) {
                byte[] a = genByteArray(i);
                byte[] b = genByteArray(j);
                boolean expected = i < j;

                boolean result = BytesAsUInt.lessThan(a, b);

            }
        });
    }
    public byte[] byteArrayOf(int... literals) {
        byte[] out = new byte[literals.length];
        for(int i = 0; i < out.length; i++) {
            out[i] = (byte)literals[i];
        }
        return out;
    }

}
