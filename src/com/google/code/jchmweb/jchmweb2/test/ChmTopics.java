package com.google.code.jchmweb.jchmweb2.test;

import com.google.code.jchmweb.jchmweb2.Configuration.ParamsClass;
import java.io.IOException;

import com.google.code.jchmweb.jchmweb2.ChmFile;

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
