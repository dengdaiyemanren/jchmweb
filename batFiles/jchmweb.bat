set JCHMLIB=D:/bin/jchmlib
set CLASSPATH=%CLASSPATH%;%JCHMLIB%/bin/jchmlib.jar
start java -Djchmweb.template=%JCHMLIB%/bin org.jchmlib.net.ChmWeb %1 %2 
