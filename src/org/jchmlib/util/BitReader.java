package org.jchmlib.util;

import java.nio.ByteBuffer;

public class BitReader {

    public static int bitBuffer;
    public static int bitsBuffered;
    public static ByteBuffer byteBuffer;
    
    public static void init(ByteBuffer bb) {
        byteBuffer = bb;
        bitBuffer = 0;
        bitsBuffered = 0;
    }

    public static void init(int bitb, int bl, ByteBuffer byteb) {
        byteBuffer = byteb;
        bitBuffer = bitb;
        bitsBuffered = bl;
    }

    public static void ensureBits(int n) {
        int bits;
        while (bitsBuffered < n) {
            bits = readc() & 0xFF;
            bitBuffer |= (bits) << (24 - bitsBuffered);
            bitsBuffered += 8;
        }
    }

    public static int peekBits(int n) {
        return (int)(bitBuffer >>> (32 - n));
    }

    public static void removeBits(int n) {
        bitBuffer <<= n;
        bitsBuffered -= n;
    }

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
