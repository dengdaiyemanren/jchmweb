/*
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 * Modified by Arthur Khusnutdinov, March 2011.
 *
 */
package org.jchmlib.net;

import java.io.UnsupportedEncodingException;

/**
 * Utility class for HTML form decoding. This class contains static methods for
 * decoding a String from the <CODE>application/x-www-form-urlencoded</CODE>
 * MIME format.
 */
public class UDecoder {

    // The platform default encoding
    static String dfltEncName = System.getProperty("file.encoding");

    public static String decode(String s) {
        return decode(s, true);
    }

    public static String decode(String s, boolean query) {
        String str = null;

        try {
            str = decode(s, dfltEncName, query);
        } catch (UnsupportedEncodingException e) {
            // The system should always have the platform default
        }

        return str;
    }

    public static String decode(String s, String enc)
            throws UnsupportedEncodingException {

        return decode(s, enc, true);
    }

    public static String decode(String s, String enc, boolean query)
            throws UnsupportedEncodingException {

        if (s == null) {
            return null;
        }

        boolean needToChange = false;
        StringBuffer sb = new StringBuffer();
        int numChars = s.length();
        int i = 0;

        if (enc.length() == 0) {
            throw new UnsupportedEncodingException(
                    "URLDecoder: empty string enc parameter");
        }

        while (i < numChars) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    if (query) {
                        sb.append(' ');
                        needToChange = true;
                    } else {
                        sb.append('+');
                    }
                    i++;
                    break;
                case '%':
                    /*
                     * Starting with this instance of %, process all consecutive
                     * substrings of the form %xy. Each substring %xy will yield a
                     * byte. Convert all consecutive bytes obtained this way to
                     * whatever character(s) they represent in the provided
                     * encoding.
                     */

                    try {

                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        byte[] bytes = new byte[(numChars - i) / 3];
                        int pos = 0;

                        while (((i + 2) < numChars) && (c == '%')) {
                            bytes[pos++] = (byte) Integer.parseInt(s.substring(i + 1, i + 3), 16);
                            i += 3;
                            if (i < numChars) {
                                c = s.charAt(i);
                            }
                        }

                        // A trailing, incomplete byte encoding such as
                        // "%x" will cause an exception to be thrown

                        if ((i < numChars) && (c == '%')) {
                            throw new IllegalArgumentException(
                                    "URLDecoder: Incomplete trailing escape (%) pattern");
                        }

                        sb.append(new String(bytes, 0, pos, enc));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "URLDecoder: Illegal hex characters in escape (%) pattern - "
                                + e.getMessage());
                    }
                    needToChange = true;
                    break;
                default:
                    sb.append(c);
                    i++;
                    break;
            }
        }

        return (needToChange ? sb.toString() : s);
    }
}
