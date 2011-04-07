package com.google.code.jchmweb.jchmweb2.util;

import com.google.code.jchmweb.jchmweb2.Configuration.ParamsClass;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Another bitstream reader.
 */
public class BitUtil {

    public static void main(String[] argv) throws IOException {
        RandomAccessFile rf = new RandomAccessFile(argv[0], "r");

        MappedByteBuffer bb = rf.getChannel().
            map(FileChannel.MapMode.READ_ONLY,0,0x150);
        init(bb);
        int i;
        do {
            i = readBits(8);
            ParamsClass.logger.fatal("8 bits = " + i + " == " + Integer.toBinaryString(i));
        } while(bb.hasRemaining() || bitsBuffered > 0) ;
    }

    public static long bitBuffer;
    public static int bitsBuffered;
    public static ByteBuffer byteBuffer;

    /**
     * should be used first to set up the system
     */
    public static void init(ByteBuffer bb) {
        byteBuffer = bb;
        bitBuffer = 0L;
        bitsBuffered = 0;
    }

    /**
     * Set up the system.
     * @param bitb bit buffer
     * @param bl   bits buffered
     * @param byteb byte buffer
     */
    public static void init(long bitb, int bl, ByteBuffer byteb) {
        byteBuffer = byteb;
        bitBuffer = bitb;
        bitsBuffered = bl;
    }

    /**
     * Ensures there are at least n bits in the bit buffer 
     */
    public static void ensureBits(int n) {
        int highbits, lowbits;
        while (bitsBuffered < n) {
            // Attention!
            lowbits = readc() & 0xFF;
            highbits = readc() & 0xFF;
            bitBuffer |= (long)((highbits << 8) | lowbits) << (48 - bitsBuffered);
            bitsBuffered += 16;
        }
    }

    /**
     * Extracts (without removing) N bits from the bit buffer 
     */
    public static int peekBits(int n) {
        return (int)(bitBuffer >>> (64 - n));
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
