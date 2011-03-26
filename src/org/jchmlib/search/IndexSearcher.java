package org.jchmlib.search;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.jchmlib.ChmFile;
import org.jchmlib.ChmFtsHeader;
import org.jchmlib.ChmUnitInfo;
import org.jchmlib.util.BitReader;

import org.jchmlib.util.ByteBufferHelper;
import org.jchmlib.util.GBKHelper;

public class IndexSearcher {
    public static void main(String[] argv) throws IOException {
        if (argv.length != 2) {
            System.out.println("Usage: java app.ChmIndexSearcher "
                    + "<chmfile> <object>");
        }

        ChmFile chmFile = new ChmFile(argv[0]);

        IndexSearcher searcher = new IndexSearcher(chmFile);
        searcher.search(argv[1], true, false);

        HashMap<String, String> results = searcher.getResults();

        if (results == null) {
            System.out.println("Object <" + argv[1] + "> not found!");
            return;
        }

        System.out.println("Object <" + argv[1] + "> found!");
        Iterator<Entry<String, String>> it = results.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            String url = entry.getKey();
            String topic = entry.getValue();
            System.out.println(topic + ":\t\t " + url);
        }
    }

    public IndexSearcher(ChmFile chmFile) {
        this.chmFile = chmFile;
    }

    public HashMap<String, String> getResults() {
        return results;
    }

    public void search(String keyword, boolean wholeWords,
            boolean titlesOnly) throws IOException {

        byte word_len, pos;
        String word = null;
        byte[] wordbytes = null;

        if (keyword.equals("")) return;

        text = keyword.toLowerCase();

        uimain = chmFile.resolveObject("/$FIftiMain");
        uitopics = chmFile.resolveObject("/#TOPICS");
        uiurltbl = chmFile.resolveObject("/#URLTBL");
        uistrings = chmFile.resolveObject("/#STRINGS");
        uiurlstr = chmFile.resolveObject("/#URLSTR");

        if (uimain == null || uitopics == null || uiurltbl == null
                || uistrings == null || uiurlstr == null) {
            System.err.println("This CHM file is unsearchable.");
            return;
        }

        ByteBuffer buf = chmFile.retrieveObject(uimain,
                0,
                ChmFile.FTS_HEADER_LEN);
        if (buf == null) return;
        buf.order(ByteOrder.LITTLE_ENDIAN);
        header = new ChmFtsHeader(buf);
        if (header == null) return;

        if (header.doc_index_s != 2 || header.code_count_s != 2
                || header.loc_codes_s != 2) return;

        int node_offset = getLeafNodeOffset();
        if (node_offset == 0) return;

        do {
            // get a leaf node here
            buf = chmFile.retrieveObject(uimain,
                    node_offset,
                    header.node_len);
            if (buf == null) return;
            buf.order(ByteOrder.LITTLE_ENDIAN);

            long wlc_count, wlc_size;
            int wlc_offset;
            // The leaf nodes begin with a short header,
            // which is followed by entries:

            // Leaf node header
            // Offset Type Comment/Value
            // 0 DWORD Offset to the next leaf node.
            // 0 if this is the last leaf node.
            // 4 WORD 0 (unknown)
            // 6 WORD Length of free space at the end
            // of the current leaf node.
            node_offset = buf.getInt();
            buf.getShort();
            short free_space = buf.getShort();
            buf.limit(header.node_len - free_space);
            while (buf.hasRemaining()) {
                word_len = buf.get();
                pos = buf.get();

                byte[] wrd_buf = new byte[word_len - 1];
                buf.get(wrd_buf);

                if (pos == 0) {
                    word = new String(wrd_buf, chmFile.codec);
                    wordbytes = wrd_buf;
                }
                else {
                    byte[] tmpbytes = new byte[pos + word_len - 1];
                    int j;
                    for (j = 0; j < pos; j++) {
                        tmpbytes[j] = wordbytes[j];
                    }
                    for (j = 0; j < word_len - 1; j++) {
                        tmpbytes[pos + j] = wrd_buf[j];
                    }
                    word = new String(tmpbytes, chmFile.codec);
                    wordbytes = tmpbytes;
                }

                // Context (0 for body tag, 1 for title tag)
                byte context = buf.get();
                wlc_count = ByteBufferHelper.parseCWord(buf);
                wlc_offset = buf.getInt();
                buf.getShort();
                wlc_size = ByteBufferHelper.parseCWord(buf);

                if ((context == 0) && titlesOnly) continue;

                // if (wholeWords && text.compareToIgnoreCase(word) == 0 ) {
                if (GBKHelper.compare(text, word) == 0) {
                    // System.out.println("!" + word + "!");
                    ProcessWLC(wlc_count, wlc_size, wlc_offset);
                    return;
                }
                else {
                    // System.out.print(word + ", ");
                    if (GBKHelper.compare(text, word) < 0) { return; }
                }

                if (!wholeWords && word.startsWith(text)) {
                    ProcessWLC(wlc_count, wlc_size, wlc_offset);
                }
            }
            // System.out.println();

        }
        while (!wholeWords && word.startsWith(text)
                && node_offset != 0);

    }

    private void ProcessWLC(long wlc_count, long wlc_size,
            int wlc_offset) throws IOException {

        int wlc_bit = 7;
        long index = 0, count;
        int off = 0;
        ByteBuffer buffer = null;
        int stroff, urloff;
        int j;
        byte tmp;

        ByteBuffer entry = null;
        ByteBuffer combuf = null;
        byte[] bytebuf = null;

        buffer = chmFile.retrieveObject(uimain, wlc_offset, wlc_size);
        if (buffer == null) {
            System.err
                    .println("Can't retrieve object:" + uimain.path);
            return;
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (long i = 0; i < wlc_count; i++) {
            if (wlc_bit != 7) {
                ++off;
                wlc_bit = 7;
            }

            BitReader.init(buffer);
            index += ByteBufferHelper.sr_int(buffer,
                    wlc_bit,
                    header.doc_index_s,
                    header.doc_index_r);
            entry = chmFile.retrieveObject(uitopics, index * 16, 16);
            if (entry == null) {
                System.err.println("Can't retrieve object:"
                        + uitopics.path);
                return;
            }
            entry.order(ByteOrder.LITTLE_ENDIAN);
            entry.getInt();
            stroff = entry.getInt();

            String topic;

            combuf = chmFile.retrieveObject(uistrings, stroff, 1024);
            if (combuf == null) {
                topic = "Untitled in index";
            }
            else {
                int size = combuf.capacity();
                bytebuf = new byte[size];
                j = 0;
                while ((tmp = combuf.get()) != 0) {
                    bytebuf[j] = tmp;
                    j++;
                }
                topic = new String(bytebuf, 0, j, chmFile.codec);
            }

            urloff = entry.getInt();

            combuf = chmFile.retrieveObject(uiurltbl, urloff, 12);
            if (combuf == null) return;

            combuf.order(ByteOrder.LITTLE_ENDIAN);
            combuf.getInt();
            combuf.getInt();
            urloff = combuf.getInt();

            combuf = chmFile.retrieveObject(uiurlstr,
                    urloff + 8,
                    1024);
            if (combuf == null) return;

            bytebuf = new byte[1024];
            j = 0;
            while ((tmp = combuf.get()) != 0) {
                bytebuf[j] = tmp;
                j++;
            }
            String url = new String(bytebuf, 0, j, chmFile.codec);

            if (!url.equals("") && !topic.equals("")) {
                if (!addResult(url, topic)) { return; }
            }

            count = ByteBufferHelper.sr_int(buffer,
                    wlc_bit,
                    header.code_count_s,
                    header.code_count_r);
            for (j = 0; j < count; j++) {
                ByteBufferHelper.sr_int(buffer,
                        wlc_bit,
                        header.loc_codes_s,
                        header.loc_codes_r);
            }
        }
    }

    private int getLeafNodeOffset() throws IOException {

        ByteBuffer buf = null;
        int test_offset = 0;
        byte word_len, pos;
        String word = null;
        int initialOffset = header.node_offset;
        int buffSize = header.node_len;
        short treeDepth = header.tree_depth;

        while ((--treeDepth) != 0) {
            if (initialOffset == test_offset) return 0;

            test_offset = initialOffset;

            buf = chmFile.retrieveObject(uimain,
                    initialOffset,
                    buffSize);
            if (buf == null) return 0;
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // The index nodes begin with a WORD indicating the
            // length of free space at the end of the node.
            // This is followed by the entries, which fill up
            // as much of the index node as possible.
            short free_space = buf.getShort();
            buf.limit(buffSize - free_space);
            while (buf.hasRemaining()) {
                word_len = buf.get();
                pos = buf.get();

                byte[] wrd_buf = new byte[word_len - 1];
                buf.get(wrd_buf);

                if (pos == 0) word = new String(wrd_buf,
                        chmFile.codec);
                else word = word.substring(0, pos)
                        + new String(wrd_buf, chmFile.codec);

                // if (text.compareToIgnoreCase(word) <= 0) {
                if (GBKHelper.compare(text, word) <= 0) {
                    // System.out.println("!!" + word);
                    initialOffset = buf.getInt();
                    break;
                }

                // System.out.println(word);
                buf.getInt();
                buf.getShort();
            }
        }

        if (initialOffset == test_offset) return 0;

        return initialOffset;
    }

    private boolean addResult(String url, String topic) {
        if (results == null) {
            results = new LinkedHashMap<String, String>();
        }
        if (results.size() < 100) {
            results.put(url, topic);
            return true;
        }
        else {
            System.err.println("Too many results.");
            return false;
        }
    }

    private ChmFile                 chmFile;

    private String                  text;

    private HashMap<String, String> results;

    private ChmUnitInfo             uimain;

    private ChmUnitInfo             uitopics;

    private ChmUnitInfo             uiurltbl;

    private ChmUnitInfo             uistrings;

    private ChmUnitInfo             uiurlstr;

    private ChmFtsHeader            header;

}
