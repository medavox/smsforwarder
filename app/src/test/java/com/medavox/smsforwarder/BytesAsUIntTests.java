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
    @Test
    public void test_increment()  {
        byte[] test = BytesAsUInt.newZeroedBytes(4);
        byte[] manualZeroes = new byte[]{(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00};
        Assert.assertTrue(Arrays.equals(test, manualZeroes));
        byte[] manualZeroOne = new byte[]{(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00};
        test = BytesAsUInt.increment(test);
        Assert.assertTrue("expected:"+bytesToHex(manualZeroOne)+"; actual:"+bytesToHex(test),
                Arrays.equals(test, manualZeroOne));
    }
}
