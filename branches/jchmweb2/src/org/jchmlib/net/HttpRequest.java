/* HttpRequest.java 2006/08/22
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 *
 */

package org.jchmlib.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

public class HttpRequest {
	/**
	 * Request METHODS.
	 */
	public static final String 
	        __GET = "GET", 
	        __POST = "POST",
			__HEAD = "HEAD", 
			__PUT = "PUT", 
			__OPTIONS = "OPTIONS",
			__DELETE = "DELETE", 
			__TRACE = "TRACE",
			__CONNECT = "CONNECT", 
			__MOVE = "MOVE";

	private BufferedReader reader;

	private String encoding;

	private URI uri = null;

	private String mimeType;

	private HashMap<String, String> paramaters;

	private boolean paramsExtracted;

	/**
	 * Constructs a HttpRequest from the given InputStream and the named
	 * encoding.
	 */
	public HttpRequest(InputStream input, String encoding) {
		this.encoding = encoding;

		try {
			reader = new BufferedReader(new InputStreamReader(input,
					encoding));
		} catch (UnsupportedEncodingException e) {
			System.err.println("Encoding " + encoding
					+ " unsupported:\n" + e);
			reader = new BufferedReader(new InputStreamReader(input));
		}
		
		try {
			readHeader(reader);
		} catch (Exception e) {
			System.err.println("Error reading request header:\n" + e);
		}
	}

	/**
	 * Read the request line and header.
	 */
	private void readHeader(BufferedReader in) throws IOException,
			URISyntaxException {
		String line_buffer = null;

		// read HTTP request -- the request comes in
		// on the first line, and is of the form:
		// GET <filename> HTTP/1.x
		do {
			line_buffer = in.readLine();
			if (line_buffer == null)
				return;
		} while (line_buffer.length() == 0);
        // ParamsClass.logger.info(line_buffer);

		String[] strs = Pattern.compile(" ").split(line_buffer);
		if (strs == null || strs.length != 3
				|| !(__GET).equals(strs[0])
				|| !(strs[2]).startsWith("HTTP/1")) {
			return;
		}

		String raw_uri = strs[1];
		uri = new URI(raw_uri);
		// loop through and discard rest of request
		do {
			line_buffer = in.readLine();
            // ParamsClass.logger.info(line_buffer);
		} while (line_buffer != null && line_buffer.length() > 0);
	}

	/**
	 * Returns the name of the character encoding used in the body 
     * of this request.
	 */
	public String getCharacterEncoding() {
		return encoding;
	}

	/**
	 * Returns the MIME type of the body of the request.
	 */
	public String getContentType() {
		if (mimeType != null)
			return mimeType;

		String path = getPath();

		// get the extention
		String ext = "";
		int indexDot = path.lastIndexOf(".");
		if (indexDot != -1)
			ext = path.substring(indexDot);
		mimeType = MimeMapper.loopupMime(ext);

		return mimeType;
	}

	/**
	 * Returns the value of a request parameter as a 
     * <code>String</code>, or <code>null</code> if the parameter 
     * does not exist.
	 */
	public String getParameter(String name) {
		if (!paramsExtracted)
			extractParameters();
		return (String) paramaters.get(name);
	}

	/**
	 * Returns Map of Parameters.
	 */
	public HashMap<String, String> getParameters() {
		if (!paramsExtracted)
			extractParameters();
		return paramaters;
	}

	/**
	 * Returns the request path.
	 */
	public String getPath() {
		String rawPath = uri.getRawPath();
		String path;
		try {
			path = UDecoder.decode(rawPath, encoding, false);
		} catch (UnsupportedEncodingException e) {
			System.err.println("Encoding " + encoding
					+ " unsupported.");
			path = UDecoder.decode(rawPath, false);
		}
		return path;
	}

	/**
	 * Retrieves the body of the request as character data using a
	 * <code> BufferedReader</code>.
	 */
	public BufferedReader getReader() {
		return reader;
	}

	/**
	 * Overrides the name of the character encoding used in the body
     * of this request.
	 */
	public void setCharacterEncoding(String env) {
		encoding = env;
	}

	/*
	 * Extract Parameters from query string.
	 */
	private void extractParameters() {
		if (paramsExtracted)
			return;
		paramsExtracted = true;

		if (paramaters == null)
			paramaters = new LinkedHashMap<String, String>();

		String query = uri.getRawQuery();
		if (query == null)
			return;
        
		// key-and-value pairs
		String[] pairs = Pattern.compile("&").split(query);
		int len = pairs.length;
		for (int i = 0; i < len; i++) {
			String[] s2 = Pattern.compile("=").split(pairs[i]);
			if (s2 != null && s2.length == 2) {
				String key, value;
				try {
					key = UDecoder.decode(s2[0], encoding, true);
					value = UDecoder.decode(s2[1], encoding, true);
				} catch (UnsupportedEncodingException e) {
					System.err.println("Encoding " + encoding
							+ " unsupported");
					key = UDecoder.decode(s2[0], true);
					value = UDecoder.decode(s2[1], true);
				}
				if (key != null && value != null) {
					paramaters.put(key, value);
                }
			}
		}
	}
}
