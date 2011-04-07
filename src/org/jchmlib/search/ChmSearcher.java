package org.jchmlib.search;

import Configuration.ParamsClass;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jchmlib.ChmFile;

public class ChmSearcher {
    public static void main(String[] argv) throws IOException {
        ChmFile chmFile = null;

        if (argv.length < 2) {
            ParamsClass.logger.fatal("Usage: ChmSearcher <chmfile> <keyword> ...");
            return;
        }

        chmFile = new ChmFile(argv[0]);
        Collection<String> results = new ArrayList<String>();
        chmFile.enumerate(ChmFile.CHM_ENUMERATE_USER, 
                new SearchEnumerator(chmFile, argv[1], results));
        if (results == null) {
            ParamsClass.logger.info("No match.");
            return;
        }
        Iterator<String> iter = results.iterator();
        while (iter.hasNext()) {
            String path = iter.next();
            ParamsClass.logger.info(path);
        }

    }
}

