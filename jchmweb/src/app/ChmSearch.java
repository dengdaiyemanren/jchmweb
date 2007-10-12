package app;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.jchmlib.ChmFile;

public class ChmSearch {
    public static void main(String[] argv) throws IOException {
        if (argv.length != 2) {
            System.out.println("Usage: ChmSearch <chmfile> <object>");
        }
        ChmFile chmFile = new ChmFile(argv[0]);
        HashMap<String, String> results = chmFile.indexSearch(argv[1],true, false);
        if (results == null) {
            System.out.println("Object <" + argv[1] + "> not found!");
        }
        else {
            System.out.println("Object <" + argv[1] + "> found!");
            Iterator<String> it = results.keySet().iterator();
            while (it.hasNext()) {
                String url = it.next();
                String topic = results.get(url);
                System.out.println(topic + ":\t\t " + url);
            }
        }
    }
}
