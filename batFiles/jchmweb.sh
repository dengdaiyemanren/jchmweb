#! /bin/sh
export JCHMLIB=/mnt/hda5/3_lab/jchmlib
export CLASSPATH=$CLASSPATH:$JCHMLIB/bin/jchmlib.jar
java -Djchmweb.template=$JCHMLIB/bin org.jchmlib.net.ChmWeb "$@" 
