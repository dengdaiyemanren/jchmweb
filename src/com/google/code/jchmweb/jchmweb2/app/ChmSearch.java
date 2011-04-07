package com.google.code.jchmweb.jchmweb2.app;

import com.google.code.jchmweb.jchmweb2.Configuration.ParamsClass;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import com.google.code.jchmweb.jchmweb2.ChmFile;

public class ChmSearch {
    public static void main(String[] argv) throws IOException {
        if (argv.length != 2) {
            ParamsClass.logger.fatal("Usage: ChmSearch <chmfile> <object>");
        }
        ChmFile chmFile = new ChmFile(argv[0]);
        HashMap<String, String> results = chmFile.indexSearch(argv[1],true, false);
        if (results == null) {
            ParamsClass.logger.info("Object <" + argv[1] + "> not found!");
        }
        else {
            ParamsClass.logger.info("Object <" + argv[1] + "> found!");
            Iterator<String> it = results.keySet().iterator();
            while (it.hasNext()) {
                String url = it.next();
                String topic = results.get(url);
                ParamsClass.logger.info(topic + ":\t\t " + url);
            }
        }
    }
}
