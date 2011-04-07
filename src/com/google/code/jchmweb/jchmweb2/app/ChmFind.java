package com.google.code.jchmweb.jchmweb2.app;

import com.google.code.jchmweb.jchmweb2.Configuration.ParamsClass;
import java.io.IOException;

import com.google.code.jchmweb.jchmweb2.ChmFile;
import com.google.code.jchmweb.jchmweb2.ChmUnitInfo;

public class ChmFind {
	public static void main(String[] argv) throws IOException {
		if (argv.length != 2) {
			ParamsClass.logger.fatal("Usage: ChmFind <chmfile> <object>");
			return;
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
