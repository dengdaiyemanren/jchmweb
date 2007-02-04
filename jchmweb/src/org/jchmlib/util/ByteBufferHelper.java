/* ByteBufferHelper.java 06/08/22
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 *
 */ 

package org.jchmlib.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * ByteBufferHelper provides some ByteBuffer-relating methods.
 *
 */
public class ByteBufferHelper
{ 
    /**
     * parse a compressed dword (a variant length integer)
     */
    public static long parseCWord(ByteBuffer bb) {
        long accum = 0;
        byte temp = bb.get();
        while (temp < 0) {
            accum <<= 7;
            accum += temp & 0x7f;
            temp = bb.get();
        }
        
        return (accum << 7) + temp;
    }
    
    /**
     * Parses a utf-8 string.
     */
    public static String parseUTF8(ByteBuffer bb, int strLen) {
        return parseString(bb, strLen, "UTF-8");
    }
    
    /**
     * Parses a String using the named Charset Encoding.
     */
    public static String parseString(ByteBuffer bb, int strLen, 
            String codec) {
        byte[] buf = new byte[strLen];
        bb.get(buf);

        String name = "";
        try {
            name = new String(buf, codec);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Encoding " + codec + " unsupported");
            name = new String(buf);
        }
        return name;
    }

    /**
     * Parse a kind of integer of variant length.
     */
    public static long sr_int(ByteBuffer bb, int bit,
           byte s, byte r)
    {
        int n_bits, count = 0;
        
        if (bit > 7 || s != 2)
            return ~(long)0;

        BitReader.init(bb);        
        while (BitReader.readBits(1) == 1) {
            count++;
        }
        
        n_bits =  r + ((count > 0) ? count - 1 : 0);
        long ret = BitReader.readBits(n_bits);
        if (count > 0)
            ret |= (long) 1 << n_bits;
        return ret;
    }
    
    /**
     * Skip the data from a PMGL entry.
     */
    public static void skipPMGLEntryData(ByteBuffer bb) {
        skipCWord(bb);
        skipCWord(bb);
        skipCWord(bb);
    }
    
    /**
     * Skip a compressed dword. 
     */
    public static void skipCWord(ByteBuffer bb) {
        while ( bb.get() < 0 )
            ;
    }

    /**
     * Gets the remaining contents of the given java.nio.ByteBuffer 
     * (i.e. the bytes between its position and limit) as a String.  
     * Leaves the position of the ByteBuffer unchanged.
     */
    public static String dataToString(ByteBuffer buf, String encoding) {
        // First check to see if the input buffer has a backing array; 
        // if so, we can just use it, to save making a copy of the data
        byte[] bytes;
        if (buf.hasArray())
        {
            bytes = buf.array();
            return bytesToString(bytes, buf.position(), buf.remaining(),
                    encoding);
        }
        else
        {
            synchronized (buf)
            {
                // Remember the original position of the buffer
                int pos = buf.position();
                bytes = new byte[buf.remaining()];
                buf.get(bytes);
                // Reset the original position of the buffer
                buf.position(pos);
            }
            return bytesToString(bytes, encoding);
        }
    }

    public static String bytesToString(byte[] bytes, 
            String encoding) {
        return bytesToString(bytes, 0, bytes.length, encoding);
    }

    public static String bytesToString(byte[] bytes, int offset, 
            int length, String encoding) {
        String result;
        try {
            result = new String(bytes, offset, length, encoding);
        }
        catch (UnsupportedEncodingException uee) {
            System.err.println("Fatal error: " + encoding + 
                    " is not supported on this platform!");
            result = new String(bytes, offset, length);
        }
        return result;
    }

    /**
     * Bitstream reader.
     */
    private static class BitReader {

        public static int bitBuffer;
        public static int bitsBuffered;
        public static ByteBuffer byteBuffer;
        
        /**
         * should be used first to set up the system
         */
        public static void init(ByteBuffer bb) {
            byteBuffer = bb;
            bitBuffer = 0;
            bitsBuffered = 0;
        }

        /**
         * Set up the system.
         * @param bitb bit buffer
         * @param bl   bits buffered
         * @param byteb byte buffer
         */
        public static void init(int bitb, int bl, ByteBuffer byteb) {
            byteBuffer = byteb;
            bitBuffer = bitb;
            bitsBuffered = bl;
        }

        /**
         * Ensures there are at least n bits in the bit buffer 
         */
        public static void ensureBits(int n) {
            int bits;
            System.out.println("So n=" + n);
            while (bitsBuffered < n) {
                bits = readc() & 0xFF;
                bitBuffer |= (bits) << (24 - bitsBuffered);
                bitsBuffered += 8;
            }
        }

        /**
         * Extracts (without removing) N bits from the bit buffer 
         */
        public static int peekBits(int n) {
            return (int)(bitBuffer >>> (32 - n));
        }

        /**
         * Removes N bits from the bit buffer.
         */
        public static void removeBits(int n) {
            bitBuffer <<= n;
            bitsBuffered -= n;
        }

        /**
         * Takes N bits from the buffer. 
         */
        public static int readBits(int n) {
            ensureBits(n);
            int result = peekBits(n);
            removeBits(n);
            return result;
        }

        public static int readc() {
            if (byteBuffer.hasRemaining()) {
                return byteBuffer.get();
            }
            return -1;
        }

    }

    
}

