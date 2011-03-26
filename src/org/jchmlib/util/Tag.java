package org.jchmlib.util;

import java.util.HashMap;

public class Tag {
    public String name;
    public String text;
    public int tagLevel;
    public int totalLevel;
    public HashMap<String, String> elems 
        = new HashMap<String, String>();
}