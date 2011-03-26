package org.jchmlib.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jchmlib.ChmFile;

public class ChmSearcher {
    public static void main(String[] argv) throws IOException {
        ChmFile chmFile = null;

        if (argv.length < 2) {
            System.out.println("Usage: ChmSearcher <chmfile> <keyword> ...");
            return;
        }

        chmFile = new ChmFile(argv[0]);
        Collection<String> results = new ArrayList<String>();
        chmFile.enumerate(ChmFile.CHM_ENUMERATE_USER, 
                new SearchEnumerator(chmFile, argv[1], results));
        if (results == null) {
            System.out.println("No match.");
            return;
        }
        Iterator<String> iter = results.iterator();
        while (iter.hasNext()) {
            String path = iter.next();
            System.out.println(path);
        }

    }
}

