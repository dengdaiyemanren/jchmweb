package org.jchmlib.util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;

public class TagReader
{
    int level;
    HashMap<String, Integer> tagLevels;
    ByteBuffer data;
    String codec;
    
    public TagReader(ByteBuffer data, String codec) {
        this.data = data;
        this.codec = codec;
        tagLevels = new HashMap<String, Integer>();
        level = 0;
    }

    public Tag getNext() {
        Tag ret = new Tag();
        ret.totalLevel = level;
        
        if (!data.hasRemaining()) return ret;
        
        String tagString = readTag();
        String istring = tagString;

        if (tagString.startsWith("<!")) { // comment or metadata, skip it
            return getNext();
        }
        if (tagString.startsWith("<?")) { // special data, skip it
            return getNext();
        }
        int itmp;
        if (tagString.startsWith("</")) { // a closed tag
            ret.name = tagString.substring(1, tagString.length()-1).trim().toLowerCase();
            level--;
            ret.totalLevel = level;
            if (tagLevels.containsKey(ret.name.substring(1))) {
                itmp = ((Integer) tagLevels.get(ret.name.substring(1))).intValue();
                itmp--;
            }
            else {
                itmp = 0;
            }
            tagLevels.put(ret.name.substring(1), new Integer(itmp));
            ret.tagLevel =  itmp;
            return ret;
        }
        
        // open tag
        ret.name = tagString.substring(1, tagString.length()-1).trim().toLowerCase();
        if (tagLevels.containsKey(ret.name)) {
            itmp = ((Integer) tagLevels.get(ret.name)).intValue();
            itmp++;
        }
        else {
            itmp = 1;
        }
        tagLevels.put(ret.name, new Integer(itmp));
        ret.tagLevel =  itmp;
        
        // now read the tag paremeters
        tagString = tagString.substring(1);
        int index = tagString.indexOf(" ");
        if (index > 0) {
            String elem;
            String value;
            ret.name = tagString.substring(0, index).toLowerCase();
            tagString = tagString.substring(index+1);
            
            int indexQuote = -1;
            int indexEq = tagString.indexOf("=");
            int i=0;
            while (indexEq > 0) {
                i++;
                elem = tagString.substring(0, indexEq);
                indexQuote = tagString.substring(indexEq + 2).indexOf("\"");
                if (indexQuote < 0) {
                    // the hhc file seems broken
                    System.out.println(tagString);
                    break;
                }
                value = tagString.substring(indexEq+2, indexEq+2+indexQuote);
                ret.elems.put(elem.toLowerCase(), value);
                
                tagString = tagString.substring(indexEq+2+indexQuote+2);
                indexEq = tagString.indexOf("=");
            }
            if (ret.name.equalsIgnoreCase("param") && i!=2) {
                System.out.println("Strange: "+istring);
            }
        }
        
        return ret;
    }
    
    public boolean hasNext() {
        return data.hasRemaining();
    }
    
    public String readTag() {
        skipWhitespace();
        
        byte[] buf = new byte[1024];
        int pos = 0;
        
        peek(); // skip '<'
        buf[pos++] = data.get();
        while (peek() != '>') {
            if (peek() == '=') {
                buf[pos++] = data.get();
                skipWhitespace();
                buf[pos++] = data.get(); // '"' after '='
                while (peek() != '"') {
                    buf[pos++] = data.get();
                }
                buf[pos++] = data.get();
            }
            else {
                buf[pos++] = data.get();
            }
        }
        buf[pos++] = data.get();
        
        skipWhitespace();
        
        String tag="";
        try {
            tag = new String(buf, 0, pos, codec);
        } catch (UnsupportedEncodingException e) {
            System.err.println("Encoding " + codec + " unsupported");
            tag = new String(buf, 0, pos);
        }
        return tag;
    }
    
    private int peek() {
        data.mark();
        int result = data.get();
        data.reset();
        
        return result;
    }
    
    public void skipWhitespace() {
        while ( hasNext() &&
                Character.isWhitespace( (char)peek() ) ) {
            data.get();
        }        
    }
}
