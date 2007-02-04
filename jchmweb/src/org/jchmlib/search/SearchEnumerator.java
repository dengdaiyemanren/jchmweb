/* SearchEnumerator.java 2006/08/22
 *
 * Copyright 2006 Chimen Chen. All rights reserved.
 *
 */

package org.jchmlib.search;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jchmlib.ChmEnumerator;
import org.jchmlib.ChmFile;
import org.jchmlib.ChmUnitInfo;
import org.jchmlib.util.ByteBufferHelper;

/**
 * SearchEnumerator uses regular expression to check whether the
 * content of a given CHM unit matches a query string.<p>
 *
 * The query string may contains one or more keywords, seperated
 * by whitespaces. The keyword can itself be a regular expression
 * and can be quoted as well.<p>
 * 
 * Here are some typical query strings:
 * <pre> abc
 * abc abc   
 * "query string" (go)*
 * Http[a-zA-Z]*Request\(.*\);
 * </pre>   
 */
public class SearchEnumerator implements ChmEnumerator {
    public static final String REGEX = "\"[^\"]*\"|[^\\s]+";

    private ChmFile chmFile;
    private Collection<String> keywords;
    private Collection<String> results;
    private boolean toManyResults;

    /**
     * Note that <code>results</code> should be initialized first. Here is an
     * example:
     * 
     * <pre>
     * Collection&lt;String&gt; results = new ArrayList&lt;String&gt;();
     * chmFile.enumerate(
     *         ChmFile.CHM_ENUMERATE_ALL, 
     *         new SearchEnumerator(chmFile, query, results));
     * </pre>
     */
    public SearchEnumerator(ChmFile chmFile, String query, 
            Collection<String> results) {

        this.chmFile = chmFile;
        this.results = results;
        keywords = new ArrayList<String>();
        
        /*
	Matcher matcher = Pattern.compile(REGEX).matcher(query); 

        while (matcher.find()) {
            String word = query.substring( matcher.start(),
                    matcher.end());

            if (word.length() > 1 && word.startsWith("\"") 
                    && word.endsWith("\"")) {
                word = word.substring(1, word.length()-1);
            }

            if ("".equals(word.trim())) continue;

            keywords.add(word);
        }
        */

	StringBuilder sb = new StringBuilder();
	int length = query.length();
	boolean quoting = false;
	char c_i;
	for (int i=0; i<length; i++) {
		c_i = query.charAt(i);
		if (c_i == '\\') {
			char c = query.charAt(i+1);
			if (c == ' ' || c == 'w' || c == 'W' 
				|| c == 's' || c == 'S'
				|| c == 'd' || c == 'D') {
				sb.append("\\");
				sb.append(c);
				i++;
			}
		} else if (c_i == '"') {
			if (!quoting) {
				quoting = true;
				sb.append("\\Q");
			} else {
				quoting = false;
				sb.append("\\E");
				if (sb.length() > 4) {
					keywords.add(new String(sb));
					// System.out.println(sb);
				}
				sb = new StringBuilder();
			}
		} else if (c_i == ' ') {
			if (quoting) {
				sb.append(' ');
			} else if (!quoting && sb.length() != 0) {
				keywords.add(new String(sb));
				// System.out.println(sb);
				sb = new StringBuilder();
			}
		} else {
			sb.append(c_i);
		}
	}
	if (sb.length() != 0) {
		keywords.add(new String(sb));
		// System.out.println(sb);
	}

        toManyResults = false;
    }

    public void enumerate(ChmUnitInfo ui) {
        if (toManyResults) return;
        
        Iterator<String> iter = keywords.iterator();
        if (!iter.hasNext()) return;

        ByteBuffer buf = null;        
        buf = chmFile.retrieveObject(ui);        
        if (buf == null) return;
        
        String data = ByteBufferHelper.dataToString(buf,
                chmFile.codec); 

        while (iter.hasNext()) {
            String keyword = iter.next();
            Pattern p = Pattern.compile(keyword);
            Matcher m = p.matcher(data);
            if (!m.find()) return;
        }

        addResult(ui);
    }

    private void addResult(ChmUnitInfo ui) {
        if (results.size() < 30) {
            results.add(ui.path);
        } else {
            toManyResults = true;
            System.err.println("Too many results.");
        }
    }
    
}
