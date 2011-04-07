#! /bin/sh
export JCHMLIB=/mnt/hda5/3_lab/jchmlib
export CLASSPATH=$CLASSPATH:$JCHMLIB/bin/jchmlib.jar
java -Djchmweb.template=$JCHMLIB/bin com.google.code.jchmweb.jchmweb2.net.ChmWeb "$@" 
