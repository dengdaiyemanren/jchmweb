/* ChmTopicsTree.java 2006/05/25
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 *
 */

package com.google.code.jchmweb.jchmweb2;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import com.google.code.jchmweb.jchmweb2.util.Tag;
import com.google.code.jchmweb.jchmweb2.util.TagReader;

/**
 * A ChmTopicsTree object contains the topics in a .chm archieve.
 * <p>
 * 
 * @author Chimen Chen
 */
public class ChmTopicsTree {
    private ChmTopicsTree() {
        title = "";
        path = "";
        children = new LinkedList<ChmTopicsTree>();
        pathToTitle = null;
    }

    /**
     * Build the top level ChmTopicsTree.
     * 
     * @param buf
     * @param encoding
     */
    public static ChmTopicsTree buildTopicsTree(ByteBuffer buf,
            String encoding) {
        ChmTopicsTree tree = new ChmTopicsTree();
        tree.pathToTitle = new LinkedHashMap<String, String>();
        tree.parent = null;
        tree.title = "<Top>";

        ChmTopicsTree curRoot = tree;
        ChmTopicsTree lastNode = tree;

        TagReader tr = new TagReader(buf, encoding);
        while (tr.hasNext()) {
            Tag s = tr.getNext();

            if (s.name.equalsIgnoreCase("ul") && s.tagLevel > 1) {
                curRoot = lastNode;
            }
            else if (s.name.equalsIgnoreCase("/ul") && s.tagLevel > 0
                    && curRoot.parent != null) {
                lastNode = curRoot;
                curRoot = curRoot.parent;
            }
            else if (s.name.equalsIgnoreCase("object")
                    && ((String) s.elems.get("type"))
                            .equalsIgnoreCase("text/sitemap")) {

                lastNode = new ChmTopicsTree();
                lastNode.parent = curRoot;
                
                s = tr.getNext();
                while (!s.name.equalsIgnoreCase("/object")) {
                    if (s.name.equalsIgnoreCase("param")) {
                        String name = (String) s.elems.get("name");
                        String value = (String) s.elems.get("value");
                        if (name == null) {
                            System.err
                                    .println("Illegal content file!");
                        }
                        else if (name.equals("Name")) {
                            lastNode.title = value;
                        }
                        else if (name.equals("Local")) {
                            if (value.startsWith("./")) {
                                value = value.substring(2);
                            }
                            lastNode.path = "/" + value;
                        }
                    }
                    s = tr.getNext();
                }

                curRoot.children.addLast(lastNode);

                if (!"".equals(lastNode.path)) {
                    tree.pathToTitle.put(lastNode.path.toLowerCase(),
                            lastNode.title);
                }
            }
        }

        return tree;

    }

    public String getTitle(String path) {
        String result = "untitled";
        if (pathToTitle != null
                && pathToTitle.containsKey(path.toLowerCase())) {
            result = (String) pathToTitle.get(path.toLowerCase());
        }
        // ParamsClass.logger.info(path + " -> " + result);
        return result;
    }

    /**
     * Title of the tree node
     */
    public String                    title;

    /**
     * Path to file under given topic or empty
     */
    public String                    path;

    /**
     * Pointer to parent tree node, null if no parent
     */
    public ChmTopicsTree             parent;

    /**
     * list of children nodes
     */
    public LinkedList<ChmTopicsTree> children;

    /**
     * Mapping from paths to titles.
     */
    private HashMap<String, String>  pathToTitle;

}
