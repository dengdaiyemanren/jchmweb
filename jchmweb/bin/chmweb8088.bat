set JCHMLIB=D:/3_lab/jchmlib
set CLASSPATH=%CLASSPATH%;%JCHMLIB%/bin/jchmlib.jar
java -Djchmweb.template=%JCHMLIB%/bin org.jchmlib.net.ChmWeb 8088 %1
