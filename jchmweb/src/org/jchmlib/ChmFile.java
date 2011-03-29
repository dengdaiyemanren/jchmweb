/* ChmFile.java 2007/10/12
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 *
 */

package org.jchmlib;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.jchmlib.search.IndexSearcher;
import org.jchmlib.util.ByteBufferHelper;
import org.jchmlib.util.EncodingHelper;
import org.jchmlib.util.LZXInflator;

/**
 * ChmFile is a class for dealing with Microsoft CHM format files 
 * (also known as Html Help files).
 *
 * @author Chimen Chen
 */

public class ChmFile {
    
    public final static int CHM_ITSF_V3_LEN  = 0X60;
    public final static int CHM_ITSP_V1_LEN  = 0X54;
    public final static int CHM_COMPRESSED   = 1;
    public final static int CHM_UNCOMPRESSED = 0;
    
    public final static int CHM_PARAM_MAX_BLOCKS_CACHED = 0;
    public final static int CHM_MAX_BLOCKS_CACHED = 5;
    
    /** Path starts with "/", but not "/#" and "/$". */
    public final static int CHM_ENUMERATE_NORMAL    =  1;

    /** Path does not start with "/". */
    public final static int CHM_ENUMERATE_META      =  2;

    /** Path starts with "/#" or "/$". */
    public final static int CHM_ENUMERATE_SPECIAL   =  4;

    /** Path does not end with "/". */
    public final static int CHM_ENUMERATE_FILES     =  8;

    /** Path ends with "/". */
    public final static int CHM_ENUMERATE_DIRS      = 16;

    /** 
     * CHM_ENUMERATE_NORMAL | 
     * CHM_ENUMERATE_FILES |
     * CHM_ENUMERATE_DIRS
     */
    public final static int CHM_ENUMERATE_USER      = 25; 

    /** 
     * CHM_ENUMERATE_NORMAL | 
     * CHM_ENUMERATE_META |
     * CHM_ENUMERATE_SPECIAL |
     * CHM_ENUMERATE_FILES |
     * CHM_ENUMERATE_DIRS
     */
    public final static int CHM_ENUMERATE_ALL       = 31;
    

    public final static int CHM_LZXC_RESETTABLE_V1_LEN = 0x28;

    public final static int FTS_HEADER_LEN = 0x32;
    
    // names of sections essential to decompression
    public static final String CHMU_RESET_TABLE =
        "::DataSpace/Storage/MSCompressed/Transform/" +
        "{7FC28940-9D31-11D0-9B27-00A0C91E9C7C}/" +
        "InstanceData/ResetTable";
    public static final String CHMU_LZXC_CONTROLDATA =
        "::DataSpace/Storage/MSCompressed/ControlData";
    public static final String CHMU_CONTENT =
        "::DataSpace/Storage/MSCompressed/Content";
    public static final String CHMU_SPANINFO =
        "::DataSpace/Storage/MSCompressed/SpanInfo";
    
    private String            file_name;
    private RandomAccessFile  rf;
    
    private long              dir_offset;
    private long              dir_len;
    private long              data_offset;
    private int               index_root;
    private int               index_head;
    private int               dir_block_len;
    private int               content_offset;
    private int               block_len;
    private long[]            resetTable;
    private int               window_size;
    private int               reset_blkcount;
    
    // decompressor
    private LZXInflator       lzxInflator;
    
    /**
     * Mapping from paths to {@link ChmUnitInfo} objects.
     */
    private HashMap<String, ChmUnitInfo>  dirMap;


    /*
     * A {@link ChmTopicsTree} object containing topics in the Chm file.
     */
    private ChmTopicsTree     tree;

    /**
     * Path of the main file (default file to show on startup).
     */
    public String            home_file;

    /** .hhc file which gives a topic tree */
    public String            topics_file;

    /** .hhk file which gives a index tree */
    public String            index_file;

    /** The title of this .chm archive */
    public String            title;

    /** The generator of this .chm archiev.
     * Normally, it would be HHA Version 4.74.8702. */ 
    public String            generator;

    /** The language codepage ID of this .chm archive */
    public int               detectedLCID = -1;

    /** The character encoding of this .chm archive */
    public String            codec = "UTF-8";
    
    /**
     * Creates a new ChmFile.
     *
     * @param       filename    the system-dependent filename of 
     *                          the CHM file
     * @exception   IOException 
     *              if the file doesnot exist or the file is of the
     *              wrong format.
     */
    public ChmFile(String filename) throws IOException {
        file_name = filename;
        rf = new RandomAccessFile(file_name, "r");
        
        readInitialHeader();
        readDirectoryHeader();
        readDirectoryTable();
        readResetTable();
        readControlData();
        initInflator();
        initMiscFiles();
        // tree = getTopicsTree(); // TODO:
    }
    
    private void readInitialHeader() throws IOException {
        ByteBuffer bb = fetchBytes(0, CHM_ITSF_V3_LEN);
        ChmItsfHeader itsfHeader = new ChmItsfHeader(bb);
        
        // stash important values from header
        dir_offset  = itsfHeader.dir_offset;
        dir_len     = itsfHeader.dir_len;
        data_offset = itsfHeader.data_offset;
        bb.clear();
    }
    
    private void readDirectoryHeader() throws IOException {
        ByteBuffer bb = fetchBytes(dir_offset, CHM_ITSP_V1_LEN);
        ChmItspHeader itspHeader = new ChmItspHeader(bb);
        
        // grab essential information from ITSP header
        dir_offset   += itspHeader.header_len;
        dir_len      -= itspHeader.header_len;
        index_root    = itspHeader.index_root;
        index_head    = itspHeader.index_head;
        dir_block_len = itspHeader.block_len;
        
        // if the index root is -1, this means we don't have any PMGI blocks.
        // as a result, we must use the sole PMGL block as the index root
        if (index_root <= -1)
            index_root = index_head;
    }
    
    private void readDirectoryTable() throws IOException {
        ByteBuffer buf = null;
        ChmPmglHeader header = null;
        // LinkedHashMap, since we want it to remember the order
        // mappings are inserted.
        dirMap = new LinkedHashMap<String, ChmUnitInfo>();
        
        // starting page
        int curPage = index_head;
        
        while (curPage != -1) {
            buf = fetchBytes(dir_offset + curPage * dir_block_len,
                dir_block_len);
            header = new ChmPmglHeader(buf);
            
            // scan directory listing entries
            while (buf.position() < dir_block_len - header.free_space) {
                // parse a PMGL entry into a ChmUnitInfo object
                ChmUnitInfo ui = new ChmUnitInfo(buf);
                dirMap.put(ui.path.toLowerCase(), ui);
                if (ui.path.endsWith(".hhc")) {
                    dirMap.put("/@contents", ui);
                }
            }
            
            // advance to next page
            curPage = header.block_next;
        }
    }
    
    private void readResetTable() throws IOException {
        // TODO: if compression is not enabled...
        // initialize content_offset first
        content_offset = (int)resolveObject(CHMU_CONTENT).start;
        
        ChmUnitInfo rt_ui = resolveObject(CHMU_RESET_TABLE);
        ByteBuffer sbuf = retrieveObject(rt_ui, 0,
            CHM_LZXC_RESETTABLE_V1_LEN);
        ChmLzxcResetTable reset_table = new ChmLzxcResetTable(sbuf);
        
        block_len = (int) reset_table.block_len;
        
        int block_count = reset_table.block_count;

        /* each entry in the reset table is 8-bytes long */
        long bytes_to_read = block_count * 8;
        if (bytes_to_read + CHM_LZXC_RESETTABLE_V1_LEN > rt_ui.length) {
        	bytes_to_read = rt_ui.length - CHM_LZXC_RESETTABLE_V1_LEN;
        	block_count = (int) bytes_to_read / 8;
        }
        sbuf = fetchBytes(data_offset + rt_ui.start 
            + reset_table.table_offset,
            bytes_to_read);
        
        resetTable = new long[block_count + 1];
        for (int i=0; i<block_count; i++) {
            resetTable[i] = data_offset + content_offset + sbuf.getLong();
        }
        resetTable[reset_table.block_count] = data_offset + content_offset +
        reset_table.compressed_len;
    }
    
    private void readControlData() throws IOException {
        ChmUnitInfo uiLzxc = resolveObject(CHMU_LZXC_CONTROLDATA);
        ByteBuffer sbuf = retrieveObject(uiLzxc, 0, uiLzxc.length);
        ChmLzxcControlData ctlData = new ChmLzxcControlData(sbuf);
        
        reset_blkcount = ctlData.resetInterval / (ctlData.windowSize / 2) *
        ctlData.windowsPerReset;
        window_size = ctlData.windowSize;
    }
    
    private void initInflator() {
        int lwindow_size = ffs(window_size) - 1;
        lzxInflator = new LZXInflator(lwindow_size);
    }
    
    private void initMiscFiles() {
        int type, len;
        String data;

        title = "JChmLib";  // default title
        ChmUnitInfo system = resolveObject("/#SYSTEM");
        ByteBuffer buf = retrieveObject(system);
        buf.getInt();
        while (buf.hasRemaining()) {
            type = buf.getShort();
            len = buf.getShort();
            // data = parseString(buf, len);
            // data = data.substring(0, data.length() -1);
            switch (type) {
                case 0:
                    data = parseString(buf, len);
                    data = data.substring(0, data.length() -1);
                    topics_file = "/" + data;
                    System.out.println("topics file: " + topics_file);
                    break;
                case 1:
                    data = parseString(buf, len);
                    data = data.substring(0, data.length() -1);
                    index_file = "/" + data;
                    System.out.println("index file: " + index_file);
                    break;
                case 2:
                    data = parseString(buf, len);
                    data = data.substring(0, data.length() -1);
                    home_file = "/" + data;
                    System.out.println("home file: " + home_file);
                    break;
                case 3:
                    title = ByteBufferHelper.parseString(buf, len, codec);
                    title = title.substring(0, title.length() -1);
                    System.out.println("title: " + title);
                    break;
                case 4:
                    detectedLCID = buf.getShort();
                    codec = EncodingHelper.findCodec(detectedLCID);
                    // System.out.println("detectedLCID:" + Integer.toHexString(detectedLCID));
                    System.out.println("Encoding: " + codec);
                    data = parseString(buf, len-2);
                    break;
                case 9:
                    data = parseString(buf, len);
                    generator = data.substring(0, data.length() -1);
                    System.out.println("Generator: " + generator);
                    break;
                default:
                    data = parseString(buf, len);
            }
        }
    }
    
    /**
     * Resolve a particular object from the Chm file.
     *
     * @param   objPath     the path of the object. It can be
     *          "::DataSpace/Storage/MSCompressed/SpanInfo"
     *          "/index.html" or "/files/", etc.
     */
    public ChmUnitInfo resolveObject(String objPath) {
        ChmUnitInfo result = null;
        if (dirMap != null && dirMap.containsKey(objPath.toLowerCase())) {
            result = (ChmUnitInfo) dirMap.get(objPath.toLowerCase());
        }
        return result;
    }
    
    /**
     * Retrieve an object.
     *
     * @param   ui  an abstract representation of the object.
     *
     */
    public ByteBuffer retrieveObject(ChmUnitInfo ui) {
        return retrieveObject(ui, 0, ui.length);
    }
    
    /**
     *  retrieve (part of) an object 
     *
     * @param   ui      an abstract representation of the object.
     * @param   addr    starting address(relative to start of the object)
     * @param   len     length(in bytes) to be retrieved.
     */
    public ByteBuffer retrieveObject(ChmUnitInfo ui, long addr, long len) {

        int swath=0;
        byte[] bytes = null;
        ByteBuffer buf = null;
        ByteBuffer lbuf = null;
        
        // starting address must be in correct range
        if (addr < 0  ||  addr >= ui.length)
            return null;
        
        // clip length
        if (addr + len > ui.length)
            len = ui.length - addr;
        
        // if the file is uncompressed, it's simple
        if (ui.space == CHM_UNCOMPRESSED) {
            // read data
            try {
                buf = fetchBytes(data_offset + ui.start + addr, len);
            }
            catch (IOException e) {
            }
        }
        else {
            do {
                try {
                    lbuf = decompressRegion(ui.start + addr, len);
                }
                catch (IOException e) {
                }

                if (lbuf == null) {
                    // System.out.println("nothing!!!!!");
                    break;
                }
                
                swath = lbuf.limit() - lbuf.position();
                bytes = new byte[swath];
                if (buf == null) {
                    buf = ByteBuffer.allocate((int)len);
                }
                while ( lbuf.hasRemaining() ) {
                    lbuf.get(bytes);
                    buf.put(bytes);
                }
                
                len -=  swath;
                addr +=  swath;
                
            } while (len > 0);
            
            if (buf != null) {
                // buf.limit(buf.position());
                buf.position(0);    // wind to the start of the buffer
            }
        }
        
        return buf;
    }
    
    /**
     * Decpmpress a region in the CHM file.
     *
     * @param   start   starting offset(relative to the start of a CHM file)
     * @param   len     length in bytes
     */
    public ByteBuffer decompressRegion(long start, long len)
        throws IOException {
        ByteBuffer buf = null;
        ByteBuffer ubuffer = null;
        int nBlock;
        long nLen, nOffset;
        
        if (len <= 0)
            return null;
        
        // figure out what we need to read
        nBlock = (int) (start / block_len);
        nOffset = start % block_len;
        
        nLen = len;
        if (nLen > (block_len - nOffset))
            nLen = block_len - nOffset;
        
        // decompress some data
        ubuffer = decompressBlock(nBlock);
        
        if (ubuffer == null) {
            return null;
        }
        
        buf = ubuffer;
        buf.position((int)nOffset);
        buf.limit((int)(nOffset + nLen));
        
        return buf;
    }
    
    /**
     * Decompress a block.
     */
    public synchronized ByteBuffer decompressBlock(int block) throws IOException {
        ByteBuffer ubuffer = null;
        ByteBuffer cbuffer = ByteBuffer.allocate(block_len + 6144);
        int  blockAlign = (int)(block % reset_blkcount); // reset intvl. aln.
        
        // check if we need previous blocks
        if (blockAlign != 0) {
            // fetch all required previous blocks since last reset
            for (int i = blockAlign; i > 0; i--) {
                int curBlockIdx = (int)(block - i);
                
                if ((curBlockIdx % reset_blkcount) == 0) {
                    lzxInflator.reset();
                }
                
                cbuffer = fetchBytes(resetTable[curBlockIdx], 
                    resetTable[curBlockIdx+1] - resetTable[curBlockIdx]);
                ubuffer = lzxInflator.decompress(cbuffer, 
                    (int)(resetTable[curBlockIdx+1] - resetTable[curBlockIdx]),
                    block_len);
            }
        }
        else {
            if ((block % reset_blkcount) == 0) {
                lzxInflator.reset();
            }
        }
        
        cbuffer = fetchBytes(resetTable[block], 
                resetTable[block+1] - resetTable[block]);
        ubuffer = lzxInflator.decompress(cbuffer, 
            (int ) (resetTable[block+1] - resetTable[block]),
            block_len);

        return ubuffer;
    }
    
    /**
     * Enumerate the objects in a directories.
     * Directories are objects whose paths end with "/".
     * If a object is direcotries ifself, we won't recursively
     * enumerate the objects in it. 
     *
     * @param   prefix  Prefix of the directories to be enumerated
     * @param   what    types of directories to be enumerated.
     *                  could be one of(or the conbination of) the following:
     *                  <ul>
     *                  <li>CHM_ENUMERATE_NORMAL,</li>
     *                  <li>CHM_ENUMERATE_META,</li>
     *                  <li>CHM_ENUMERATE_SPECIAL,</li>
     *                  <li>CHM_ENUMERATE_FILES,</li>
     *                  <li>CHM_ENUMERATE_DIRS,</li>
     *                  <li>CHM_ENUMERATE_ALL,</li>
     *                  <li>CHM_ENUMERATE_USER.</li>
     *                  </ul>
     * @param   e       the enumerator which does something on the enumerated
     *                  objects(like callback function in C/C++).
     */ 
    public void enumerateDir(String prefix, int what, ChmEnumerator e) 
    throws IOException {
        ChmUnitInfo ui = null;
        
        // set to true once we've started
        boolean it_has_begun = false;
        
        int type_bits = (what & 0x7);
        int filter_bits = (what & 0xF8);
        
        // initialize pathname state
        if (!prefix.endsWith("/")) {
            prefix = prefix.concat("/");
        }
        
        String lastPath = null;
        
        Iterator<ChmUnitInfo> it = dirMap.values().iterator();
        while (it.hasNext()) {
            ui = it.next();
            //check if we should start
            if (!it_has_begun) {
                if (ui.length == 0 && ui.path.startsWith(prefix)) {
                    it_has_begun = true;
                    continue;
                }
                else 
                    continue;
            }
            // check if we should stop
            else if (!ui.path.startsWith(prefix)) {
                return;
            }
            
            // check if we should include this path
            if (lastPath != null && ui.path.startsWith(lastPath)) {
                continue;
            }
            lastPath = ui.path;
            
            if ((type_bits & ui.flags) == 0)
                continue;
            
            if (filter_bits != 0 && (filter_bits & ui.flags) == 0)
                continue;
            
            // call the enumerator
            e.enumerate(ui);                    
        }
    }
    
    /**
     * Enumerate the objects in the .chm archive.
     *
     * @param   what    types of objects to be enumerated.
     *                  could be one of(or the conbination of) the following:
     *                  <ul>
     *                  <li>CHM_ENUMERATE_NORMAL,</li>
     *                  <li>CHM_ENUMERATE_META,</li>
     *                  <li>CHM_ENUMERATE_SPECIAL,</li>
     *                  <li>CHM_ENUMERATE_FILES,</li>
     *                  <li>CHM_ENUMERATE_DIRS,</li>
     *                  <li>CHM_ENUMERATE_ALL,</li>
     *                  <li>CHM_ENUMERATE_USER.</li>
     *                  </ul>
     * @param   e       the enumerator which does something on the enumerated
     *                  objects(like callback function in C/C++).
     */
    public void enumerate(int what, ChmEnumerator e) throws IOException {
        ChmUnitInfo ui = null;
        
        int type_bits = (what & 0x7);
        int filter_bits = (what & 0xF8);
        
        Iterator<ChmUnitInfo> it = dirMap.values().iterator();
        while (it.hasNext()) {
            ui = it.next();
            
            if ((type_bits & ui.flags) == 0)
                continue;
            
            if (filter_bits != 0 && (filter_bits & ui.flags) == 0)
                continue;
            
            // call the enumerator
            e.enumerate(ui);                    
        }
    }

    /**
     * Retrieves the ChmTopicsTree of this .chm archive.
     */
    public ChmTopicsTree getTopicsTree() {
        if (tree != null) return tree;

        ChmUnitInfo ui = null;
        ByteBuffer buf = null;
        ui = resolveObject("/@contents");
        if (ui == null) return null;
        // System.out.println(ui.path + " Size: " + ui.length);
        buf = retrieveObject(ui, 0, ui.length);

        tree = ChmTopicsTree.buildTopicsTree(buf, codec);
      
        return tree;
    }

    /**
     * Gets the title of a given path.
     *
     * @param path path of a Chm object (namely, a chm unit).
     */
    public String getTitle(String path) {
        if (tree == null) {
            getTopicsTree();
        }
        String title = tree.getTitle(path);
        tree = null;
        return title;
    }

    public void releaseTopicsTree() {
        tree = null;
    }

    /**
     * Prints the ChmTopicsTree of this .chm archive.
     */
    public static void printTopicsTree(ChmTopicsTree tree, int level) {
        if (tree == null) return;

        for (int i=0; i<level; i++) {
            System.out.print("    ");
        }

        System.out.println(tree.title + "\t" + tree.path);

        Iterator<ChmTopicsTree> it = tree.children.iterator();
        while (it.hasNext()) {
            ChmTopicsTree child = (ChmTopicsTree) it.next();
            printTopicsTree(child, level+1);
        }
    }
    
    /**
     * Perform a full-text search on a .chm archive.
     * To do this, the .chm archive should be searchable.
     *
     * @param text keyword.
     * @param wholeWords if false, matches indics that starts with text.
     * @param titlesOnly if true, search titles only; 
     *                   
     */
    public HashMap<String, String> indexSearch(String text, boolean wholeWords,
            boolean titlesOnly) throws IOException {
        IndexSearcher searcher = new IndexSearcher(this);
        searcher.search(text, wholeWords, titlesOnly);

        return searcher.getResults();
    }

    private ByteBuffer fetchBytes(long offset, long len)
    throws IOException {
        ByteBuffer buf = rf.getChannel()
        .map(FileChannel.MapMode.READ_ONLY, offset, len);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf;
    }

    /**
     * Parse a string using default encoding.
     * <p>
     * The default encoding is originally set to "UTF-8",
     * and is gradually set to the actual encoding of the
     * .chm file in <code>initMiscFiles()</code>.
     * 
     */
    public String parseString(ByteBuffer bb, int strLen) {
        return ByteBufferHelper.parseString(bb, strLen, codec);
    }

    
    /**
     * Find first bit set in a word.
     */
    public static int ffs(int val) {
        int bit=1, idx=1;
        while (bit != 0  &&  (val & bit) == 0) {
            bit <<= 1;
            ++idx;
        }
        if (bit == 0)
            return 0;
        else
            return idx;
    }
    
}

