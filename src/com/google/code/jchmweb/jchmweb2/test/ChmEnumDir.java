package com.google.code.jchmweb.jchmweb2.test;

import com.google.code.jchmweb.jchmweb2.Configuration.ParamsClass;
import java.io.IOException;
import java.io.PrintStream;

import com.google.code.jchmweb.jchmweb2.ChmEnumerator;
import com.google.code.jchmweb.jchmweb2.ChmFile;
import com.google.code.jchmweb.jchmweb2.ChmUnitInfo;

public class ChmEnumDir {
    public static void main(String[] argv) throws IOException {
        ChmFile chmFile = null;
        
        if (argv.length < 1) {
            ParamsClass.logger.fatal("Usage: ChmEnumDir <chmfile> [dir] [dir] ...");
            return;
        }

        chmFile = new ChmFile(argv[0]);
       
        if ( argv.length < 2) {
            ParamsClass.logger.info("/:");
            ParamsClass.logger.info(" spc\tstart\tlength\ttype\t\tname");
            ParamsClass.logger.info(" ===\t=====\t======\t====\t\t====");
            chmFile.enumerateDir("/", ChmFile.CHM_ENUMERATE_ALL, 
                    new DirEnumerator(System.out));
        }
        else {
            for (int i = 1; i < argv.length; i++) {
                ParamsClass.logger.info(argv[i] + ":");
                ParamsClass.logger.info(" spc\tstart\tlength\tname");
                ParamsClass.logger.info(" ===\t=====\t======\t====");
                chmFile.enumerateDir(argv[i], ChmFile.CHM_ENUMERATE_ALL, 
                        new DirEnumerator(System.out));
                
            }
        }
                  
    }
}

class DirEnumerator implements ChmEnumerator {

    PrintStream out;

    public DirEnumerator(PrintStream out) {
        this.out = out;
    }

    public void enumerate(ChmUnitInfo ui) {
    	String szBuf = null;
        if((ui.flags & ChmFile.CHM_ENUMERATE_NORMAL) != 0)
            szBuf = "normal ";
        else if((ui.flags & ChmFile.CHM_ENUMERATE_SPECIAL) != 0)
            szBuf = "special ";
        else if((ui.flags & ChmFile.CHM_ENUMERATE_META) != 0)
            szBuf = "meta ";
        
        if((ui.flags & ChmFile.CHM_ENUMERATE_DIRS) != 0)
            szBuf = szBuf.concat("dir");
        else if((ui.flags & ChmFile.CHM_ENUMERATE_FILES) != 0)
            szBuf = szBuf.concat("file");

        out.println(" " + ui.space + "\t" + ui.start + "\t" +
           ui.length + "\t" + szBuf + "\t" + ui.path);
    }
}