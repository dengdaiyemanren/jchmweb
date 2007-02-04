package org.jchmlib;

import java.nio.ByteBuffer;

/**
 * ITSF header.
 */
class ChmItsfHeader {
    String signature;        /*  0 (ITSF) */
    int    version;          /*  4 */
    int    header_len;       /*  8 */
    int    unknown_000c;     /*  c */
    int    last_modified;    /* 10 */
    int    lang_id;          /* 14 */
    String dir_uuid;         /* 18 */
    String stream_uuid;      /* 28 */
    long   unknown_offset;   /* 38 */
    long   unknown_len;      /* 40 */
    long   dir_offset;       /* 48 */
    long   dir_len;          /* 50 */
    long   data_offset;      /* 58 (Not present before V3) */

    public ChmItsfHeader(ByteBuffer bb) {
        byte[] sbuf = new byte[4];
        bb.get(sbuf);
        signature = new String(sbuf);

        version        = bb.getInt();
        header_len     = bb.getInt();
        unknown_000c   = bb.getInt();
        last_modified  = bb.getInt();
        lang_id        = bb.getInt();
        bb.get(new byte[32]);
        unknown_len    = bb.getLong();
        unknown_offset = bb.getLong();
        dir_offset     = bb.getLong();
        dir_len        = bb.getLong();
        data_offset    = 0; // TODO:
        if (version >= 3) {
            data_offset = bb.getLong();
        }
    }

    public String toString() {
        return signature + 
            "\n\t version:        " + Integer.toHexString(version) + 
            "\n\t header_len:     " + Integer.toHexString(header_len) +
            "\n\t lang_id:        " + Integer.toHexString(lang_id) + 
            "\n\t unknown_offset: " + Long.toHexString(unknown_offset) + 
            "\n\t unknown_len:    " + Long.toHexString(unknown_len) + 
            "\n\t dir_offset:     " + Long.toHexString(dir_offset) + 
            "\n\t dir_len:        " + Long.toHexString(dir_len) + 
            "\n\t data_offset:    " + Long.toHexString(data_offset);
    }
}
