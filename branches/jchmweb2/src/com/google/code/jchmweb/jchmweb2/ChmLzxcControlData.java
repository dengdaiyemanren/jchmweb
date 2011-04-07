package com.google.code.jchmweb.jchmweb2;

import java.nio.ByteBuffer;

/**
 * LZXC control data block.
 */
public class ChmLzxcControlData
{
    public int     size;                   /*  0        */
    public String  signature;              /*  4 (LZXC) */
    public int     version;                /*  8        */
    public int     resetInterval;          /*  c        */
    public int     windowSize;             /* 10        */
    public int     windowsPerReset;        /* 14        */
    public int     unknown_18;             /* 18        */

    public ChmLzxcControlData(ByteBuffer bb) {
        size = bb.getInt();
        byte[] sbuf = new byte[4];
        bb.get(sbuf);
        signature = new String(sbuf);
        version = bb.getInt();
        resetInterval = bb.getInt();
        windowSize = bb.getInt();
        windowsPerReset = bb.getInt();
        // TODO: something about CHM_LZXC_V2_LEN
        if (version == 2) {
            resetInterval *= 0x8000; // TODO: unsigned
            windowSize *= 0x8000;    // TODO: unsigned
        }
        // TODO: verify
    }

    public String toString() {
        // return signature + 
        //     "\n\tsize:            " + Integer.toHexString(size) + 
        //     "\n\tversion:         " + Integer.toHexString(version) +
        //     "\n\tresetInterval:   " + Integer.toHexString(resetInterval) + 
        //     "\n\twindowSize:      " + Integer.toHexString(windowSize) + 
        //     "\n\twindowsPerReset: " + Integer.toHexString(windowsPerReset);
        return signature + 
            "\n\tsize:            " + size + 
            "\n\tversion:         " + version +
            "\n\tresetInterval:   " + resetInterval + 
            "\n\twindowSize:      " + windowSize + 
            "\n\twindowsPerReset: " + windowsPerReset;
    }    
    
}
