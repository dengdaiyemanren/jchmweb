package org.jchmlib.test;

import Configuration.ParamsClass;
import java.io.IOException;

import org.jchmlib.ChmFile;
import org.jchmlib.ChmUnitInfo;

public class ChmFind {
	public static void main(String[] argv) throws IOException {
		if (argv.length != 2) {
			ParamsClass.logger.fatal("Usage: ChmFind <chmfile> <object>");
		}
		ChmFile chmFile = new ChmFile(argv[0]);
		ChmUnitInfo ui = chmFile.resolveObject(argv[1]);
		if (ui == null) {
			ParamsClass.logger.info("Object <" + argv[1] + "> not found!");
		}
		else {
			ParamsClass.logger.info("Object <" + argv[1] + "> found!");
		}
	}
}
