package app;

import Configuration.ParamsClass;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;

import org.jchmlib.ChmEnumerator;
import org.jchmlib.ChmFile;
import org.jchmlib.ChmUnitInfo;

public class ChmExtract {
    public static void main(String[] argv) throws IOException {
        ChmFile chmFile = null;
        
        if (argv.length < 1) {
            ParamsClass.logger.fatal("Usage: ChmExtract <chmfile> <outdir>");
            return;
        }

    	long time = System.currentTimeMillis (), time_prev = time;

        chmFile = new ChmFile(argv[0]);
       
        ParamsClass.logger.info("/:" + argv[0]);
        chmFile.enumerate(ChmFile.CHM_ENUMERATE_ALL, 
               new Extractor(chmFile, argv[1]));
        time = System.currentTimeMillis();
        ParamsClass.logger.info("    finished in " + (time - time_prev) + " ms");
    }
}

class Extractor implements ChmEnumerator {

    String basePath;
    ChmFile chmFile;


    public Extractor(ChmFile chmFile, String basePath) {
    	this.chmFile = chmFile;
    	if (basePath.endsWith("/")) {
            this.basePath = basePath.substring(0, 
                basePath.length() -1);
        } else {
            this.basePath = basePath;
        }
    }

    public void enumerate(ChmUnitInfo ui) {
    	PrintStream out = null;
    	ByteBuffer buffer = null;
    	byte[] bytes = null;
    	int gotLen;
    	String fullPath = basePath;

        if (!ui.path.startsWith("/"))
            return;

        fullPath = fullPath.concat(ui.path);

        if (ui.length != 0) {
//            ParamsClass.logger.info("--> " + fullPath);
            try {
            	out = new PrintStream(fullPath);
            }catch (IOException e) {
                ParamsClass.logger.fatal("   fail while opening the newly created file "
                        + ui.path);
            }
            if (out == null) {
            	ParamsClass.logger.info("   fail to open the newly created file "
                        + ui.path);
                return;
            }

            buffer = chmFile.retrieveObject(ui, 0, ui.length);
            if (buffer == null) {
                ParamsClass.logger.fatal("    extract failed on " + ui.path);
                return;
            }
            gotLen = buffer.limit() - buffer.position();
            bytes = new byte[gotLen];

            buffer.mark();
            while ( buffer.hasRemaining()) {
                 buffer.get(bytes);
                 out.write(bytes, 0, gotLen);
            }
            buffer.reset();
            out.close();            
        }
        else {
            if (fullPath.endsWith("/")) {
                new File(fullPath).mkdirs();
            }
            else {
            	new File(fullPath).delete();
            }
        }
    }
}
