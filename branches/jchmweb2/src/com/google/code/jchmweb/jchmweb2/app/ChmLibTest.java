package com.google.code.jchmweb.jchmweb2.app;

import com.google.code.jchmweb.jchmweb2.Configuration.ParamsClass;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import com.google.code.jchmweb.jchmweb2.ChmFile;
import com.google.code.jchmweb.jchmweb2.ChmUnitInfo;

public class ChmLibTest {
        
    public static void main(String[] argv) throws IOException {
            ChmFile chmFile = null;
            ChmUnitInfo ui = null;
            ByteBuffer buffer = null;
            PrintStream out = null;
            int gotLen;
            
        if (argv.length < 3) {
            ParamsClass.logger.fatal("Usage: ChmLibTest <chmfile> <filename> <destfile>");
            return;
        }

        chmFile = new ChmFile(argv[0]);
        
        ParamsClass.logger.info("Resolving " + argv[1]);

        ui = chmFile.resolveObject(argv[1]);
        if (ui != null) {

            ParamsClass.logger.info("Extracting to " + argv[2]);
            buffer = chmFile.retrieveObject(ui, 0, ui.length);
            if (buffer == null) {
                ParamsClass.logger.info("    extract failed on " + ui.path);
                return;
            }
                        
            out = new PrintStream(argv[2]);
            if (out == null) {
                ParamsClass.logger.info("    create failed on " + ui.path);
            }

            gotLen = buffer.limit() - buffer.position();
            byte[] bytes = new byte[gotLen];

            buffer.mark();
            while ( buffer.hasRemaining()) {
                 buffer.get(bytes);
                 out.write(bytes, 0, gotLen);
            }
            buffer.reset();
            out.close();
            ParamsClass.logger.info("   finished");
        }
        else {
            ParamsClass.logger.fatal("   failed");
        }
        
    }
}
