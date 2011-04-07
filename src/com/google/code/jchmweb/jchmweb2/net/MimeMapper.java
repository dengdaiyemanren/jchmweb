/* MimeMapper.java 06/08/22
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 *
 */

package com.google.code.jchmweb.jchmweb2.net;

/**
 * Mime-mapping (from extension to mime-type).
 */
class MimeMapping
{
    private String ext;
    private String ctype;

    public MimeMapping(String ext, String ctype) {
        this.ext = ext;
        this.ctype = ctype;
    }

    /**
     * Gets the extension.
     */
    public String getExtension() {
        return ext;
    }

    /**
     * Gets the mime type.
     */
    public String getMimeType() {
        return ctype;
    }

    /**
     * Sets the extension.
     */
    public void setExtension(String extension) {
        ext = extension;
    }

    /**
     * Sets the mime type.
     */
    public void setMimeType(String mimeType) {
        ctype = mimeType;
    }
}

public class MimeMapper
{
    private static final MimeMapping[] mimeTypes =
    {   new MimeMapping(".htm",  "text/html"    ),
        new MimeMapping(".html", "text/html"    ),
        new MimeMapping(".css",  "text/css"     ),
        new MimeMapping(".gif",  "image/gif"    ),
        new MimeMapping(".jpg",  "image/jpeg"   ),
        new MimeMapping(".jpeg", "image/jpeg"   ),
        new MimeMapping(".jpe",  "image/jpeg"   ),
        new MimeMapping(".bmp",  "image/bitmap" ),
        new MimeMapping(".png",  "image/png"    )
    };

    /**
     * Returns the MIME type of the named extension. 
     */
    public static String loopupMime(String ext) {
        int len = mimeTypes.length;
        for (int i = 0; i < len; i++) {
            if (ext.toLowerCase().equals(mimeTypes[i].getExtension())) {
                return mimeTypes[i].getExtension();
            }
        }        
        return "application/octet-stream";
    } 
}

