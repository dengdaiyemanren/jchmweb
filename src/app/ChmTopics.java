package app;

import Configuration.ParamsClass;
import java.io.IOException;

import org.jchmlib.ChmFile;

public class ChmTopics
{
    public static void main(String[] argv) throws IOException {
        if (argv.length < 1) {
            ParamsClass.logger.fatal("Usage: ChmTopics <chmfilename>");
            return;
        }
        ChmFile chmFile = new ChmFile(argv[0]);       
        ChmFile.printTopicsTree(chmFile.getTopicsTree(), 0);
    }

}
