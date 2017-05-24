package com.medavox.smsforwarder;

import android.util.Log;

import java.io.PrintStream;
import java.math.BigInteger;

import com.medavox.util.io.Bytes;
import com.medavox.util.io.StringUtils;

import static com.medavox.util.validate.Validator.check;

/**binChaSMS encoding:
 * uses all the printable characters from SMS to represent binary data.
 *
 * TODO:
 * tests have indicated that the numbers stage is identical at the encode and decode stages.
 * This means that it's the BigInt stage that is going wrong*/
public class BinChasmsCodec {

    private static final String TAG = "BinChaSMS encoder";
    final static int HEADER_SIZE = 4;
    final static int BYTES_PER_TEXT = 139;
    private final static int SEPTETS_PER_TEXT  = 160;

    private static final String gsmChars = "@£$¥èéùìòÇ"+
        /*LF+*/"Øø"+/*CR+*/"ÅåΔ_ΦΓΛΩΠΨΣΘΞ"+/*ESC+*/"ÆæßÉ"+" "+/*SP*/
        "!\"#¤%&'()*+,-./0123456789:; <=>?¡ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÑÜ§¿abcdefghij" +
            "klmnopqrstuvwxyzäöñüà";
    private static int CHARS = gsmChars.length();//should be 125
    private static BigInteger CHARS_BI = BigInteger.valueOf(CHARS);


    public static int calculateBytesThatCanFit(int numberOfSymbols, int uniqueSymbols) {
        //System.out.println("chars:"+CHARS);
        BigInteger combosOfChars = BigInteger.valueOf(uniqueSymbols).pow(numberOfSymbols);
        BigInteger n256 = BigInteger.valueOf(256);
        //combinationsOf140Bytes = combinationsOf140Bytes.pow(140);
        int bytesThatFit = -1;
        for(int i = 1; i < Integer.MAX_VALUE; i++) {
            BigInteger combosOfNBytes = n256.pow(i);
            if(combosOfNBytes.compareTo(combosOfChars) > 0) {
                bytesThatFit = i-1;
                //the previous power was the last & highest one to be less, therefore fit
                System.out.println("number of full bytes that can be expressed by "+numberOfSymbols
                        +" consecutive symbols with "+uniqueSymbols+" possible values: "+bytesThatFit);
                System.out.println("256^"+bytesThatFit+": "+combosOfNBytes);
                System.out.println(uniqueSymbols+"^"+numberOfSymbols+": "+combosOfChars);

                System.out.println("diff:    "+combosOfChars.subtract(combosOfNBytes));
                break;
            }
        }
        return bytesThatFit;
    }
        
        //the results are: we can represent 139 bytes using 160 characters in an sms,
            // where we only use the printable subset of 125 values
        
        //System.out.println("combinations of 140 bytes:"+combinationsOf140Bytes);
        
        //reserve 4 bytes for header: centiseconds (1 centisecond = 10 milliseconds) since sender started sending

         
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
        //fixme:any leading zero-bytes are omitted. we can't just pad the bytes with zeroes,
        //because we don't know how long the original was (<=139 bytes)
        //byte[] bytes = asNumber.toByteArray();
        //check(bytes.length <= )
        //if(bytes.length )
    }
}
